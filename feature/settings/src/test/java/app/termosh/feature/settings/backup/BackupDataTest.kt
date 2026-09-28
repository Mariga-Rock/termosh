package app.termosh.feature.settings.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDataTest {

    @Test
    fun `round trip config without secrets`() {
        val data = BackupData(
            format = "termosh",
            formatVersion = 1,
            appVersion = "0.1.0",
            exportedAt = 12345L,
            includesSecrets = false,
            servers = listOf(
                BackupServer(
                    id = "s1",
                    name = "prod",
                    host = "1.2.3.4",
                    port = 22,
                    username = "root",
                    tags = listOf("work"),
                    authType = "PASSWORD",
                    passwordPlain = null,
                    keyIds = null,
                    proxyJumpId = null,
                    useMosh = false,
                    totpSecretId = null,
                    startupCommands = listOf("cd /tmp"),
                    envVars = mapOf("EDITOR" to "vim"),
                    envSecretsPlain = emptyMap(),
                ),
            ),
            keys = emptyList(),
            snippets = emptyList(),
            knownHosts = emptyList(),
            portForwards = emptyList(),
        )
        val json = data.toJson()
        val back = BackupData.fromJson(json)
        assertFalse(back.includesSecrets)
        assertEquals(1, back.servers.size)
        assertEquals("prod", back.servers[0].name)
        assertNull(back.servers[0].passwordPlain)
        assertEquals(listOf("cd /tmp"), back.servers[0].startupCommands)
        assertEquals(mapOf("EDITOR" to "vim"), back.servers[0].envVars)
    }

    @Test
    fun `round trip vault with secrets`() {
        val data = BackupData(
            format = "termoshvault",
            formatVersion = 1,
            appVersion = "0.1.0",
            exportedAt = 999L,
            includesSecrets = true,
            servers = listOf(
                BackupServer(
                    id = "s1",
                    name = "prod",
                    host = "1.2.3.4",
                    port = 22,
                    username = "root",
                    tags = emptyList(),
                    authType = "PASSWORD",
                    passwordPlain = "secret",
                    keyIds = null,
                    proxyJumpId = null,
                    useMosh = true,
                    totpSecretId = "t1",
                    startupCommands = emptyList(),
                    envVars = emptyMap(),
                    envSecretsPlain = mapOf("AWS_KEY" to "abcdef"),
                ),
            ),
            keys = listOf(
                BackupKey(
                    id = "k1",
                    type = "ED25519",
                    privateKeyPkcs8Base64 = "AAAA",
                    publicKeyOpenSsh = "ssh-ed25519 AAAA test",
                    fingerprintSha256 = "SHA256:aaa",
                    comment = "test",
                    createdAt = 1L,
                ),
            ),
            snippets = listOf(
                BackupSnippet(id = "sn1", name = "logs", command = "tail -f /var/log/syslog", sortOrder = 0),
            ),
            knownHosts = emptyList(),
            portForwards = emptyList(),
        )
        val json = data.toJson()
        val back = BackupData.fromJson(json)
        assertTrue(back.includesSecrets)
        assertEquals("secret", back.servers[0].passwordPlain)
        assertEquals(mapOf("AWS_KEY" to "abcdef"), back.servers[0].envSecretsPlain)
        assertTrue(back.servers[0].useMosh)
        assertEquals("t1", back.servers[0].totpSecretId)
        assertEquals("AAAA", back.keys[0].privateKeyPkcs8Base64)
        assertEquals(1, back.snippets.size)
    }

    @Test
    fun `rejects unknown format`() {
        val json = """{"format":"unknown","formatVersion":1,"servers":[],"keys":[],"snippets":[],"knownHosts":[],"portForwards":[]}"""
        try {
            BackupData.fromJson(json)
            throw AssertionError("Should have failed")
        } catch (t: Throwable) {
            // ok
        }
    }

    @Test
    fun `rejects newer formatVersion`() {
        val json = """{"format":"termosh","formatVersion":99,"servers":[],"keys":[],"snippets":[],"knownHosts":[],"portForwards":[]}"""
        try {
            BackupData.fromJson(json)
            throw AssertionError("Should have failed")
        } catch (t: Throwable) {
            // ok
        }
    }
}
