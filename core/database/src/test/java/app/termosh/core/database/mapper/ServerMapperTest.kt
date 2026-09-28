package app.termosh.core.database.mapper

import app.termosh.core.database.entity.AuthType
import app.termosh.core.database.entity.ServerEntity
import app.termosh.domain.model.ServerAuth
import app.termosh.domain.model.SecretRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerMapperTest {

    @Test
    fun `password auth round trip`() {
        val entity = ServerEntity(
            id = "s1", name = "prod", host = "h", port = 22, username = "u",
            authType = AuthType.PASSWORD,
            passwordIv = byteArrayOf(1, 2, 3),
            passwordCiphertext = byteArrayOf(4, 5, 6),
            keyIdsJson = "[]", proxyJumpId = null, tags = "a,b,c",
            createdAt = 100L, lastUsedAt = null, sortOrder = 0,
            useMosh = false, totpSecretId = null, useJumpCredentials = false,
            startupCommandsJson = "[]", envVarsJson = "{}", envSecretsJson = "{}",
        )
        val domain = ServerMapper.toDomain(entity)
        assertTrue(domain.auth is ServerAuth.Password)
        assertEquals(listOf("a", "b", "c"), domain.tags)

        val back = ServerMapper.toEntity(domain)
        assertEquals(entity.id, back.id)
        assertEquals(AuthType.PASSWORD, back.authType)
        assertTrue(back.passwordIv!!.contentEquals(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun `key auth round trip with multiple keys`() {
        val entity = ServerEntity(
            id = "s2", name = "n", host = "h", port = 22, username = "u",
            authType = AuthType.KEY,
            passwordIv = null, passwordCiphertext = null,
            keyIdsJson = """["k1","k2","k3"]""", proxyJumpId = null, tags = "",
            createdAt = 100L, lastUsedAt = null, sortOrder = 0,
            useMosh = false, totpSecretId = null, useJumpCredentials = false,
            startupCommandsJson = "[]", envVarsJson = "{}", envSecretsJson = "{}",
        )
        val domain = ServerMapper.toDomain(entity)
        assertTrue(domain.auth is ServerAuth.Keys)
        assertEquals(listOf("k1", "k2", "k3"), (domain.auth as ServerAuth.Keys).keyIds)
        assertTrue(domain.tags.isEmpty())
    }

    @Test
    fun `startup commands round trip`() {
        val entity = ServerEntity(
            id = "s3", name = "n", host = "h", port = 22, username = "u",
            authType = AuthType.PASSWORD,
            passwordIv = byteArrayOf(1), passwordCiphertext = byteArrayOf(2),
            keyIdsJson = "[]", proxyJumpId = null, tags = "",
            createdAt = 100L, lastUsedAt = null, sortOrder = 0,
            useMosh = false, totpSecretId = null, useJumpCredentials = false,
            startupCommandsJson = """["cd /tmp","ls -la"]""",
            envVarsJson = """{"EDITOR":"vim","LANG":"en_US.UTF-8"}""",
            envSecretsJson = "{}",
        )
        val domain = ServerMapper.toDomain(entity)
        assertEquals(listOf("cd /tmp", "ls -la"), domain.startupCommands)
        assertEquals(mapOf("EDITOR" to "vim", "LANG" to "en_US.UTF-8"), domain.envVars)
    }

    @Test
    fun `env secrets round trip`() {
        val entity = ServerEntity(
            id = "s4", name = "n", host = "h", port = 22, username = "u",
            authType = AuthType.PASSWORD,
            passwordIv = byteArrayOf(1), passwordCiphertext = byteArrayOf(2),
            keyIdsJson = "[]", proxyJumpId = null, tags = "",
            createdAt = 100L, lastUsedAt = null, sortOrder = 0,
            useMosh = false, totpSecretId = null, useJumpCredentials = false,
            startupCommandsJson = "[]", envVarsJson = "{}",
            envSecretsJson = """{"AWS_KEY":{"iv":"AQID","ct":"BAUG"}}""",
        )
        val domain = ServerMapper.toDomain(entity)
        assertTrue(domain.envSecrets.containsKey("AWS_KEY"))
        val ref = domain.envSecrets["AWS_KEY"]!!
        assertTrue(ref.iv.contentEquals(byteArrayOf(1, 2, 3)))
        assertTrue(ref.ciphertext.contentEquals(byteArrayOf(4, 5, 6)))
    }

    @Test
    fun `useMosh and totp round trip`() {
        val entity = ServerEntity(
            id = "s5", name = "n", host = "h", port = 22, username = "u",
            authType = AuthType.PASSWORD,
            passwordIv = byteArrayOf(1), passwordCiphertext = byteArrayOf(2),
            keyIdsJson = "[]", proxyJumpId = null, tags = "",
            createdAt = 100L, lastUsedAt = null, sortOrder = 0,
            useMosh = true, totpSecretId = "totp-42", useJumpCredentials = true,
            startupCommandsJson = "[]", envVarsJson = "{}", envSecretsJson = "{}",
        )
        val domain = ServerMapper.toDomain(entity)
        assertTrue(domain.useMosh)
        assertEquals("totp-42", domain.totpSecretId)
        assertTrue(domain.useJumpCredentials)
    }
}
