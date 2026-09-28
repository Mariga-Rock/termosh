package app.termosh.core.ssh.imports

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * Парсер XML-экспорта ConnectBot.
 *
 * Формат (упрощённо):
 * <hosts>
 *   <host nickname="prod" hostname="1.2.3.4" port="22"
 *         username="root" password="..." pubkeyid="..." />
 * </hosts>
 *
 * Пароль экспортируется как plaintext в старых версиях ConnectBot.
 * В новых — только метаданные.
 */
object ConnectBotImporter {

    data class ParsedHost(
        val nickname: String,
        val hostname: String,
        val port: Int,
        val username: String,
        val passwordPlain: String?,
        val keyFingerprint: String?,
        val useCompression: Boolean,
        val useAuthAgent: Boolean,
    )

    fun parse(xml: String): List<ParsedHost> {
        val result = mutableListOf<ParsedHost>()
        val factory = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = false
        }
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "host") {
                val nickname = parser.getAttributeValue(null, "nickname") ?: ""
                val hostname = parser.getAttributeValue(null, "hostname") ?: ""
                val port = parser.getAttributeValue(null, "port")?.toIntOrNull() ?: 22
                val username = parser.getAttributeValue(null, "username") ?: ""
                val password = parser.getAttributeValue(null, "password")?.takeIf { it.isNotEmpty() }
                val pubkeyid = parser.getAttributeValue(null, "pubkeyid")?.takeIf { it.isNotEmpty() }
                val compression = parser.getAttributeValue(null, "compression") == "true"
                val authAgent = parser.getAttributeValue(null, "useauthagent") == "yes"

                if (hostname.isNotEmpty()) {
                    result += ParsedHost(
                        nickname = nickname.ifEmpty { hostname },
                        hostname = hostname,
                        port = port,
                        username = username.ifEmpty { "root" },
                        passwordPlain = password,
                        keyFingerprint = pubkeyid,
                        useCompression = compression,
                        useAuthAgent = authAgent,
                    )
                }
            }
            event = parser.next()
        }
        return result
    }
}
