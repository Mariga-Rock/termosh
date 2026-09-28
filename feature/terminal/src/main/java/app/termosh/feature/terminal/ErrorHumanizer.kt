package app.termosh.feature.terminal

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Превращает техническое исключение в фразу, понятную человеку.
 *
 * Стектрейс остаётся в логе — для диагностики. Пользователю показываем
 * короткое сообщение, из которого понятно, что делать.
 */
object ErrorHumanizer {

    fun humanize(t: Throwable): String {
        // разбираем всю цепочку cause-ов — sshj часто оборачивает настоящую причину
        val chain = generateSequence(t) { it.cause }.toList()

        for (e in chain) {
            val msg = e.message.orEmpty()
            val cls = e.javaClass.simpleName

            // Аутентификация
            if (cls == "UserAuthException" ||
                msg.contains("Exhausted available authentication", ignoreCase = true) ||
                msg.contains("Auth fail", ignoreCase = true) ||
                msg.contains("Authentication failed", ignoreCase = true)
            ) {
                return "Неверный пароль или SSH-ключ. Проверьте логин, пароль и настройки аутентификации."
            }

            if (cls == "UnknownHostException") {
                return "Хост не найден. Проверьте адрес сервера и подключение к интернету."
            }

            if (cls == "ConnectException") {
                return if (msg.contains("refused", ignoreCase = true)) {
                    "Сервер не отвечает. Порт закрыт или сервер выключен."
                } else {
                    "Не удалось установить соединение."
                }
            }

            if (cls == "SocketTimeoutException") {
                return "Таймаут соединения. Возможно, сервер за firewall или нужен VPN."
            }

            if (cls == "NoRouteToHostException") {
                return "Нет маршрута до сервера. Проверьте сеть или VPN."
            }

            // Host key — либо MITM, либо сервер переустановили
            if (msg.contains("host key", ignoreCase = true) ||
                msg.contains("fingerprint", ignoreCase = true) ||
                msg.contains("HostKey", ignoreCase = true)
            ) {
                return "Отпечаток ключа сервера изменился. " +
                    "Если сервер не переустанавливали — возможна MITM-атака, проверьте с администратором."
            }

            // BouncyCastle
            if (msg.contains("no such algorithm", ignoreCase = true)) {
                return "Сервер использует неподдерживаемый алгоритм шифрования."
            }
        }

        // Fallback
        val raw = t.message?.takeIf { it.isNotBlank() } ?: t.javaClass.simpleName
        return "Не удалось подключиться: $raw"
    }
}
