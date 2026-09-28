package app.termosh.core.security.model

enum class SshKeyType(val jcaName: String) {
    ED25519("Ed25519"),
    RSA("RSA"),
    ECDSA_P256("EC"),
}
