package app.termosh.core.ssh.imports

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

/**
 * Парсер XML-экспорта ConnectBot.
 *
 * Формат (пример):
 * ```xml
 * <?xml version="1.0" encoding="utf-8"?>
 * <forwards>
 *   <host nickname="prod" hostname="1.2.3.4" port="22" username="root"
 *         password="secret" pubkeyid="k1" usePubkey="true" />
 *   <host nickname="staging" hostname="example.com" port="2222" username="admin" />
 *   <pubkey id="k1" private="-----BEGIN..." public="ssh-rsa AAA..." nickname="mykey" />
 * </forwards>
 * ```
 *
 * Пароль может быть пустым или отсутствовать — тогда сервер импортируется с пустым секретом.
 * Если указан pubkeyid — ищем соответствующий pubkey и импортируем приватный ключ.
 */
object ConnectBotXmlParser {

    data class ParsedHost(
        val nickname: String,
        val hostname: String,
        val port: Int,
        val username: String,
        val password: String?,
        val pubkeyId: String?,
        val usePubkey: Boolean,
    )

    data class ParsedPubkey(
        val id: String,
        val privateKeyOpenSsh: String?,
        val publicKeyOpenSsh: String?,
        val nickname: String?,
    )

    data class Parsed(
        val hosts: List<ParsedHost>,
        val pubkeys: List<ParsedPubkey>,
    )

    fun parse(xml: String): Parsed {
        val hosts = mutableListOf<ParsedHost>()
        val keys = mutableListOf<ParsedPubkey>()

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name.lowercase()) {
                    "host" -> {
                        val h = readHost(parser)
                        if (h != null && h.hostname.isNotBlank()) hosts += h
                    }
                    "pubkey" -> {
                        val k = readPubkey(parser)
                        if (k != null) keys += k
                    }
                }
            }
            event = parser.next()
        }
        return Parsed(hosts, keys)
    }

    private fun readHost(p: XmlPullParser): ParsedHost? {
        val nickname = p.getAttributeValue(null, "nickname") ?: ""
        val hostname = p.getAttributeValue(null, "hostname") ?: return null
        val port = p.getAttributeValue(null, "port")?.toIntOrNull() ?: 22
        val username = p.getAttributeValue(null, "username") ?: "root"
        val password = p.getAttributeValue(null, "password")?.takeIf { it.isNotBlank() }
        val pubkeyId = p.getAttributeValue(null, "pubkeyid")?.takeIf { it.isNotBlank() }
        val usePubkey = p.getAttributeValue(null, "usePubkey")?.toBooleanStrictOrNull() ?: (pubkeyId != null)
        return ParsedHost(nickname, hostname, port, username, password, pubkeyId, usePubkey)
    }

    private fun readPubkey(p: XmlPullParser): ParsedPubkey? {
        val id = p.getAttributeValue(null, "id") ?: return null
        val private = p.getAttributeValue(null, "private")?.takeIf { it.isNotBlank() }
        val public = p.getAttributeValue(null, "public")?.takeIf { it.isNotBlank() }
        val nickname = p.getAttributeValue(null, "nickname")?.takeIf { it.isNotBlank() }
        return ParsedPubkey(id, private, public, nickname)
    }
}
