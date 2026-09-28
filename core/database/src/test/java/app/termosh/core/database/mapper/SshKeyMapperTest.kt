package app.termosh.core.database.mapper

import app.termosh.core.database.entity.SshKeyEntity
import app.termosh.domain.model.SshKeyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SshKeyMapperTest {

    @Test
    fun `round trip ED25519`() {
        val entity = SshKeyEntity(
            id = "k1",
            type = SshKeyType.ED25519.name,
            encryptedPrivateKeyIv = byteArrayOf(1, 2),
            encryptedPrivateKeyCiphertext = byteArrayOf(3, 4),
            publicKeyOpenSsh = "ssh-ed25519 AAAA test",
            fingerprintSha256 = "SHA256:aaa",
            comment = "test",
            createdAt = 1L,
        )
        val domain = SshKeyMapper.toDomain(entity)
        assertEquals(SshKeyType.ED25519, domain.type)
        assertTrue(domain.privateKeyRef.iv.contentEquals(byteArrayOf(1, 2)))
        assertTrue(domain.privateKeyRef.ciphertext.contentEquals(byteArrayOf(3, 4)))

        val back = SshKeyMapper.toEntity(domain)
        assertEquals(entity.id, back.id)
        assertEquals(SshKeyType.ED25519.name, back.type)
        assertEquals("ssh-ed25519 AAAA test", back.publicKeyOpenSsh)
    }

    @Test
    fun `round trip RSA with null comment`() {
        val entity = SshKeyEntity(
            id = "k2",
            type = SshKeyType.RSA.name,
            encryptedPrivateKeyIv = byteArrayOf(5),
            encryptedPrivateKeyCiphertext = byteArrayOf(6),
            publicKeyOpenSsh = "ssh-rsa BBBB",
            fingerprintSha256 = "SHA256:bbb",
            comment = null,
            createdAt = 2L,
        )
        val domain = SshKeyMapper.toDomain(entity)
        assertEquals(SshKeyType.RSA, domain.type)
        assertEquals(null, domain.comment)
    }
}
