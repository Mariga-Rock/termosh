package app.termosh.core.ssh.model

enum class SshSessionState {
    IDLE,
    CONNECTING,
    AUTHENTICATING,
    CONNECTED,
    DISCONNECTED,
    ERROR,
}
