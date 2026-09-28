package app.termosh.core.common

import android.content.Context
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Простой append-only журнал событий.
 * Формат: JSON Lines. Каждая запись содержит prevHash и hash
 * для обнаружения подмены (tamper-evident, не tamper-proof).
 *
 * Пароли и секреты не пишутся никогда.
 */
object AuditLog {

    private const val FILE_NAME = "audit.log"
    private val tsFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    @Volatile private var dir: File? = null
    @Volatile private var file: File? = null
    @Volatile private var lastHash: String = ""

    fun init(context: Context) {
        val d = File(context.filesDir, "audit").apply { mkdirs() }
        dir = d
        file = File(d, FILE_NAME)
        lastHash = readLastHash() ?: ""
    }

    fun auditFile(): File? = file

    fun log(event: String, fields: Map<String, Any?> = emptyMap()) {
        val f = file ?: return
        val ts = tsFmt.format(Date())
        val map = LinkedHashMap<String, Any?>()
        map["ts"] = ts
        map["event"] = event
        map["prevHash"] = lastHash
        fields.forEach { (k, v) -> if (v != null) map[k] = v }

        val json = toJson(map)
        val hash = sha256(lastHash + json)
        val withHash = json.dropLast(1) + ",\"hash\":\"$hash\"}"
        synchronized(this) {
            runCatching {
                f.appendText(withHash + "\n")
                lastHash = hash
            }
        }
    }

    fun readAll(): String = runCatching { file?.readText() ?: "" }.getOrDefault("")

    fun clear() {
        runCatching { file?.delete() }
        lastHash = ""
    }

    private fun readLastHash(): String? {
        val f = file ?: return null
        if (!f.exists()) return null
        val lastLine = f.readLines().lastOrNull { it.isNotBlank() } ?: return null
        val idx = lastLine.lastIndexOf("\"hash\":\"")
        if (idx < 0) return null
        val rest = lastLine.substring(idx + 8)
        return rest.substringBefore('"')
    }

    private fun sha256(s: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }

    private fun toJson(map: Map<String, Any?>): String {
        val sb = StringBuilder("{")
        var first = true
        map.forEach { (k, v) ->
            if (!first) sb.append(",")
            first = false
            sb.append("\"").append(escape(k)).append("\":")
            when (v) {
                null -> sb.append("null")
                is Number -> sb.append(v)
                is Boolean -> sb.append(v)
                else -> sb.append("\"").append(escape(v.toString())).append("\"")
            }
        }
        sb.append("}")
        return sb.toString()
    }

    private fun escape(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
}
