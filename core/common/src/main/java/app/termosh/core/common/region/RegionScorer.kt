package app.termosh.core.common.region

import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.telephony.TelephonyManager
import java.util.Locale
import java.util.TimeZone

/**
 * Региональный скоринг. Полностью офлайн, без разрешений.
 *
 * Бета-логика: granted = русскоязычная локаль И (часовой пояс РФ ИЛИ MCC РФ).
 * В публичной версии будет поринг по очкам с fallback-диалогом.
 */
object RegionScorer {

    fun calculate(context: Context): ScoreResult {
        val signals = linkedMapOf<String, Int>()

        // 1. Locale (40)
        val locales = LocaleList.getDefault()
        val hasRuLocale = (0 until locales.size()).any { i ->
            locales[i].language.lowercase(Locale.ROOT) in RegionData.RUSSIAN_LANGUAGES
        }
        signals["locale"] = if (hasRuLocale) 40 else 0

        // 2. Timezone (30)
        val tzId = TimeZone.getDefault().id
        val hasRuTz = tzId in RegionData.RUSSIAN_TIMEZONES
        signals["timezone"] = if (hasRuTz) 30 else 0

        // 3. MCC (20)
        val mcc = runCatching {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val op = tm?.networkOperator ?: ""
            if (op.length >= 3) op.substring(0, 3) else ""
        }.getOrDefault("")
        val hasRuMcc = mcc in RegionData.RUSSIAN_MCCS
        signals["mcc"] = if (hasRuMcc) 20 else 0

        // 4. Fingerprint (10)
        val fp = Build.FINGERPRINT.lowercase(Locale.ROOT)
        val hasFp = RegionData.FINGERPRINT_MARKERS.any { fp.contains(it) }
        signals["fingerprint"] = if (hasFp) 10 else 0

        // 5. GMS отсутствует (10)
        val hasGms = runCatching {
            context.packageManager.getPackageInfo("com.google.android.gms", 0)
            true
        }.getOrDefault(false)
        signals["no_gms"] = if (!hasGms) 10 else 0

        val total = signals.values.sum()
        val granted = hasRuLocale && (hasRuTz || hasRuMcc)

        val reason = buildString {
            append("locale=").append(if (hasRuLocale) "ru" else "other")
            append(", tz=").append(tzId)
            append(", mcc=").append(mcc.ifEmpty { "-" })
            append(", fp=").append(if (hasFp) "yes" else "no")
            append(", gms=").append(if (hasGms) "yes" else "no")
        }

        return ScoreResult(total, signals, granted, reason)
    }
}
