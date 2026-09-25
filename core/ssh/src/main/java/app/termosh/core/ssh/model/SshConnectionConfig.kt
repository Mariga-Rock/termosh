package app.termosh.core.ssh.model

/**
 * Конфигурация SSH-подключения.
 * Пароль и ключ передаются отдельно — см. SshAuthMethod.
 */
data class SshConnectionConfig(
    val host: String,
    val port: Int = 22,
    val username: String,
    val connectTimeoutMs: Int = 15_000,
    val readTimeoutMs: Int = 30_000,
    val keepAliveIntervalSec: Int = 30,
    val proxyJump: ProxyJumpConfig? = null,
)

data class ProxyJumpConfig(
    val host: String,
    val port: Int = 22,
    val username: String,
    val auth: SshAuthMethod,
)
