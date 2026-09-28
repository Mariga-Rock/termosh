package app.termosh.core.database.mapper

import app.termosh.core.database.entity.AuthType
import app.termosh.core.database.entity.ServerEntity
import app.termosh.domain.model.Server
import app.termosh.domain.model.ServerAuth
import app.termosh.domain.model.SecretRef
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

object ServerMapper {

    fun toDomain(entity: ServerEntity): Server {
        val auth: ServerAuth = when (entity.authType) {
            AuthType.PASSWORD -> {
                val iv = entity.passwordIv
                val ct = entity.passwordCiphertext
                require(iv != null && ct != null) {
                    "Server ${entity.id} has PASSWORD auth but no ciphertext"
                }
                ServerAuth.Password(SecretRef(iv = iv, ciphertext = ct))
            }
            AuthType.KEY -> {
                val ids = parseStringArray(entity.keyIdsJson)
                if (ids.isEmpty()) error("Server ${entity.id} has KEY auth but no keys")
                ServerAuth.Keys(ids)
            }
        }

        return Server(
            id = entity.id,
            name = entity.name,
            host = entity.host,
            port = entity.port,
            username = entity.username,
            auth = auth,
            proxyJumpId = entity.proxyJumpId,
            tags = entity.tags.split(',').map { it.trim() }.filter { it.isNotEmpty() },
            createdAt = entity.createdAt,
            lastUsedAt = entity.lastUsedAt,
            useMosh = entity.useMosh,
            totpSecretId = entity.totpSecretId,
            useJumpCredentials = entity.useJumpCredentials,
            startupCommands = parseStringArray(entity.startupCommandsJson),
            envVars = parseStringMap(entity.envVarsJson),
            envSecrets = parseSecretMap(entity.envSecretsJson),
        )
    }

    fun toEntity(domain: Server, sortOrder: Int = 0): ServerEntity {
        var passwordIv: ByteArray? = null
        var passwordCiphertext: ByteArray? = null
        var keyIdsJson: String = "[]"
        val authType: AuthType

        when (val a = domain.auth) {
            is ServerAuth.Password -> {
                authType = AuthType.PASSWORD
                passwordIv = a.secretRef.iv
                passwordCiphertext = a.secretRef.ciphertext
            }
            is ServerAuth.Keys -> {
                authType = AuthType.KEY
                keyIdsJson = stringArrayToJson(a.keyIds)
            }
        }

        return ServerEntity(
            id = domain.id,
            name = domain.name,
            host = domain.host,
            port = domain.port,
            username = domain.username,
            authType = authType,
            passwordIv = passwordIv,
            passwordCiphertext = passwordCiphertext,
            keyIdsJson = keyIdsJson,
            proxyJumpId = domain.proxyJumpId,
            tags = domain.tags.joinToString(","),
            createdAt = domain.createdAt,
            lastUsedAt = domain.lastUsedAt,
            sortOrder = sortOrder,
            useMosh = domain.useMosh,
            totpSecretId = domain.totpSecretId,
            useJumpCredentials = domain.useJumpCredentials,
            startupCommandsJson = stringArrayToJson(domain.startupCommands),
            envVarsJson = stringMapToJson(domain.envVars),
            envSecretsJson = secretMapToJson(domain.envSecrets),
        )
    }

    // ---- JSON helpers ----

    private fun parseStringArray(json: String): List<String> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())

    private fun stringArrayToJson(list: List<String>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun parseStringMap(json: String): Map<String, String> = runCatching {
        val obj = JSONObject(json)
        obj.keys().asSequence().associateWith { obj.getString(it) }
    }.getOrDefault(emptyMap())

    private fun stringMapToJson(map: Map<String, String>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    private fun parseSecretMap(json: String): Map<String, SecretRef> = runCatching {
        val obj = JSONObject(json)
        val b64 = Base64.getDecoder()
        obj.keys().asSequence().associateWith { key ->
            val entry = obj.getJSONObject(key)
            SecretRef(
                iv = b64.decode(entry.getString("iv")),
                ciphertext = b64.decode(entry.getString("ct")),
            )
        }
    }.getOrDefault(emptyMap())

    private fun secretMapToJson(map: Map<String, SecretRef>): String {
        val obj = JSONObject()
        val b64 = Base64.getEncoder()
        map.forEach { (k, ref) ->
            val entry = JSONObject()
            entry.put("iv", b64.encodeToString(ref.iv))
            entry.put("ct", b64.encodeToString(ref.ciphertext))
            obj.put(k, entry)
        }
        return obj.toString()
    }
}
