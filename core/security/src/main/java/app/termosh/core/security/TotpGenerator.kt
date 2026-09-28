package app.termosh.core.security

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RFC 6238 TOTP. Стандартный алгоритм: HMAC-SHA1, 6 цифр, 30 секунд.
 */
@Singleton
class TotpGenerator @Inject constructor() {

    fun generate(
        secret: ByteArray,
        timestampMs: Long = System.currentTimeMillis(),
        digits: Int = 6,
        periodSec: Int = 30,
        algorithm: String = "SHA1",
    ): String {
        val counter = timestampMs / 1000L / periodSec
        return hotp(secret, counter, digits, algorithm)
    }

    fun secondsRemaining(periodSec: Int = 30, timestampMs: Long = System.currentTimeMillis()): Int {
        val elapsed = (timestampMs / 1000L) % periodSec
        return (periodSec - elapsed).toInt()
    }

    private fun hotp(secret: ByteArray, counter: Long, digits: Int, algorithm: String): String {
        val macAlgo = "Hmac" + algorithm.uppercase()
        val mac = Mac.getInstance(macAlgo)
        mac.init(SecretKeySpec(secret, macAlgo))

        val counterBytes = ByteArray(8)
        var c = counter
        for (i in 7 downTo 0) {
            counterBytes[i] = (c and 0xFF).toByte()
            c = c shr 8
        }

        val hash = mac.doFinal(counterBytes)
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        val otp = binary % powerOf10(digits)
        return otp.toString().padStart(digits, '0')
    }

    private fun powerOf10(n: Int): Int {
        var r = 1
        repeat(n) { r *= 10 }
        return r
    }

    /**
     * Декодирование base32 (без padding), как в Google Authenticator.
     */
    fun base32Decode(input: String): ByteArray {
        val cleaned = input.replace(" ", "").replace("-", "").uppercase().trimEnd('=')
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        var buffer = 0L
        var bitsLeft = 0
        val out = java.io.ByteArrayOutputStream()
        for (ch in cleaned) {
            val idx = alphabet.indexOf(ch)
            require(idx >= 0) { "Invalid base32 char: $ch" }
            buffer = (buffer shl 5) or idx.toLong()
            bitsLeft += 5
            if (bitsLeft >= 8) {
                out.write(((buffer shr (bitsLeft - 8)) and 0xFF).toInt())
                bitsLeft -= 8
            }
        }
        return out.toByteArray()
    }
}
