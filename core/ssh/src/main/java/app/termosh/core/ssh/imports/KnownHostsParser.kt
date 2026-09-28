package app.termosh.core.ssh.imports

import java.security.MessageDigest
import java.util.Base64

/**
 * Парсер ~/.ssh/known_hosts.
 * Поддерживает открытые и хешированные (|1|salt|hash) строки.
 */
object KnownHostsParser {

    sealed interface Entry {
        val keyType: String
        val keyBase64: String
        val fingerprintSha256: String

        data class Plain(
            val host: String,
            val port: Int,
            override val keyType: String,
            override val keyBase64: String,
            override val fingerprintSha256: String,
        ) : Entry

        data class Hashed(
            val saltB64: String,
            val hashB64: String,
            override val keyType: String,
            override val keyBase64: String,
            override val fingerprintSha256: String,
        ) : Entry
    }

    fun parse(text: String): List<Entry> {
        val result = mutableListOf<Entry>()
        text.lineSequence().forEach { raw ->
            val line = raw.substringBefore('#').trim()
            if (line.isEmpty()) return@forEach

            val parts = line.split(Regex("\\s+"))
            if (parts.size < 3) return@forEach
            val hostField = parts[0]
            val keyType = parts[1]
            val keyBase64 = parts[2]

            val keyBytes = runCatching { Base64.getDecoder().decode(keyBase64) }.getOrNull()
                ?: return@forEach
            val digest = MessageDigest.getInstance("SHA-256").digest(keyBytes)
            val fp = "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(digest)

            if (hostField.startsWith("|")) {
                val segs = hostField.split("|")
                // ["", "1", salt, hash]
                if (segs.size != 4 || segs[1] != "1") return@forEach
                result += Entry.Hashed(
                    saltB64 = segs[2],
                    hashB64 = segs[3],
                    keyType = keyType,
                    keyBase64 = keyBase64,
                    fingerprintSha256 = fp,
                )
            } else {
                hostField.split(",").forEach { h ->
                    val (host, port) = splitHostPort(h)
                    result += Entry.Plain(host, port, keyType, keyBase64, fp)
                }
            }
        }
        return result
    }

    private fun splitHostPort(s: String): Pair<String, Int> {
        if (s.startsWith("[")) {
            val end = s.indexOf(']')
            if (end > 0) {
                val host = s.substring(1, end)
                val portStr = s.substring(end + 1).removePrefix(":")
                return host to (portStr.toIntOrNull() ?: 22)
            }
        }
        return s to 22
    }
}
