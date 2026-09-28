package app.termosh.core.database.mapper

/**
 * Мост между domain.SshKeyType и security.SshKeyType.
 * Оба enum'а независимы, чтобы domain не зависел от :core:security.
 */
object SshKeyTypeBridge {

    fun toSecurity(t: app.termosh.domain.model.SshKeyType): app.termosh.core.security.model.SshKeyType =
        when (t) {
            app.termosh.domain.model.SshKeyType.ED25519 ->
                app.termosh.core.security.model.SshKeyType.ED25519
            app.termosh.domain.model.SshKeyType.RSA ->
                app.termosh.core.security.model.SshKeyType.RSA
            app.termosh.domain.model.SshKeyType.ECDSA_P256 ->
                app.termosh.core.security.model.SshKeyType.ECDSA_P256
        }
}
