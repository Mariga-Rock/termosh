package app.termosh.core.database.mapper

import org.junit.Assert.assertEquals
import org.junit.Test

class SshKeyTypeBridgeTest {

    @Test
    fun `domain ED25519 maps to security ED25519`() {
        val r = SshKeyTypeBridge.toSecurity(app.termosh.domain.model.SshKeyType.ED25519)
        assertEquals(app.termosh.core.security.model.SshKeyType.ED25519, r)
    }

    @Test
    fun `domain RSA maps to security RSA`() {
        val r = SshKeyTypeBridge.toSecurity(app.termosh.domain.model.SshKeyType.RSA)
        assertEquals(app.termosh.core.security.model.SshKeyType.RSA, r)
    }

    @Test
    fun `domain ECDSA_P256 maps to security ECDSA_P256`() {
        val r = SshKeyTypeBridge.toSecurity(app.termosh.domain.model.SshKeyType.ECDSA_P256)
        assertEquals(app.termosh.core.security.model.SshKeyType.ECDSA_P256, r)
    }
}
