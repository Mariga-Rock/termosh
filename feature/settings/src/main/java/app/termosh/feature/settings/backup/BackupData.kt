package app.termosh.feature.settings.backup

import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val format: String = "termosh",
    val formatVersion: Int = 1,
    val appVersion: String = "0.1.0",
    val exportedAt: Long = System.currentTimeMillis(),
    val includesSecrets: Boolean = false,
    val servers: List<BackupServer>,
    val keys: List<BackupKey>,
    val snippets: List<BackupSnippet>,
    val knownHosts: List<BackupKnownHost>,
    val portForwards: List<BackupPortForward>,
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("format", format)
        root.put("formatVersion", formatVersion)
        root.put("appVersion", appVersion)
        root.put("exportedAt", exportedAt)
        root.put("includesSecrets", includesSecrets)

        val sArr = JSONArray()
        servers.forEach { s ->
            val o = JSONObject()
            o.put("id", s.id)
            o.put("name", s.name)
            o.put("host", s.host)
            o.put("port", s.port)
            o.put("username", s.username)
            o.put("tags", JSONArray(s.tags))
            o.put("authType", s.authType)
            if (s.passwordPlain != null) o.put("password", s.passwordPlain)
            if (s.keyIds != null && s.keyIds.isNotEmpty()) o.put("keyIds", JSONArray(s.keyIds))
            if (s.proxyJumpId != null) o.put("proxyJumpId", s.proxyJumpId)
            if (s.useMosh) o.put("useMosh", true)
            if (s.totpSecretId != null) o.put("totpSecretId", s.totpSecretId)
            if (s.startupCommands.isNotEmpty()) o.put("startupCommands", JSONArray(s.startupCommands))
            if (s.envVars.isNotEmpty()) {
                val ev = JSONObject()
                s.envVars.forEach { (k, v) -> ev.put(k, v) }
                o.put("envVars", ev)
            }
            if (s.envSecretsPlain.isNotEmpty()) {
                val ev = JSONObject()
                s.envSecretsPlain.forEach { (k, v) -> ev.put(k, v) }
                o.put("envSecrets", ev)
            }
            if (s.createdAt > 0) o.put("createdAt", s.createdAt)
            if (s.lastUsedAt != null) o.put("lastUsedAt", s.lastUsedAt)
            sArr.put(o)
        }
        root.put("servers", sArr)

        val kArr = JSONArray()
        keys.forEach { k ->
            val o = JSONObject()
            o.put("id", k.id)
            o.put("type", k.type)
            if (k.privateKeyPkcs8Base64 != null) o.put("privateKeyBase64", k.privateKeyPkcs8Base64)
            o.put("publicKeyOpenSsh", k.publicKeyOpenSsh)
            o.put("fingerprintSha256", k.fingerprintSha256)
            if (k.comment != null) o.put("comment", k.comment)
            o.put("createdAt", k.createdAt)
            kArr.put(o)
        }
        root.put("keys", kArr)

        val snArr = JSONArray()
        snippets.forEach { sn ->
            val o = JSONObject()
            o.put("id", sn.id)
            o.put("name", sn.name)
            o.put("command", sn.command)
            o.put("sortOrder", sn.sortOrder)
            snArr.put(o)
        }
        root.put("snippets", snArr)

        val hArr = JSONArray()
        knownHosts.forEach { h ->
            val o = JSONObject()
            o.put("host", h.host)
            o.put("port", h.port)
            o.put("fingerprintSha256", h.fingerprintSha256)
            o.put("addedAt", h.addedAt)
            hArr.put(o)
        }
        root.put("knownHosts", hArr)

        val pArr = JSONArray()
        portForwards.forEach { p ->
            val o = JSONObject()
            o.put("id", p.id)
            o.put("serverId", p.serverId)
            o.put("localPort", p.localPort)
            o.put("remoteHost", p.remoteHost)
            o.put("remotePort", p.remotePort)
            o.put("enabled", p.enabled)
            o.put("createdAt", p.createdAt)
            pArr.put(o)
        }
        root.put("portForwards", pArr)

        return root.toString()
    }

    companion object {
        fun fromJson(json: String): BackupData {
            val root = JSONObject(json)
            val format = root.optString("format", "termosh")
            val formatVersion = root.optInt("formatVersion", 1)
            if (format != "termosh" && format != "termoshvault") {
                error("Неизвестный формат: $format")
            }
            if (formatVersion > 1) {
                error("Формат версии $formatVersion не поддерживается этой версией приложения")
            }
            val appVersion = root.optString("appVersion", "unknown")
            val exportedAt = root.optLong("exportedAt", 0L)
            val includesSecrets = root.optBoolean("includesSecrets", false)

            val servers = mutableListOf<BackupServer>()
            val sArr = root.optJSONArray("servers") ?: JSONArray()
            for (i in 0 until sArr.length()) {
                val o = sArr.getJSONObject(i)
                val tags = mutableListOf<String>()
                o.optJSONArray("tags")?.let { t ->
                    for (j in 0 until t.length()) tags += t.getString(j)
                }
                val keyIds = mutableListOf<String>()
                o.optJSONArray("keyIds")?.let { t ->
                    for (j in 0 until t.length()) keyIds += t.getString(j)
                }
                val startup = mutableListOf<String>()
                o.optJSONArray("startupCommands")?.let { t ->
                    for (j in 0 until t.length()) startup += t.getString(j)
                }
                val envVars = mutableMapOf<String, String>()
                o.optJSONObject("envVars")?.let { ev ->
                    ev.keys().forEach { k -> envVars[k] = ev.getString(k) }
                }
                val envSecrets = mutableMapOf<String, String>()
                o.optJSONObject("envSecrets")?.let { ev ->
                    ev.keys().forEach { k -> envSecrets[k] = ev.getString(k) }
                }
                servers += BackupServer(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    host = o.getString("host"),
                    port = o.getInt("port"),
                    username = o.getString("username"),
                    tags = tags,
                    authType = o.getString("authType"),
                    passwordPlain = if (o.has("password")) o.getString("password") else null,
                    keyIds = if (keyIds.isEmpty()) null else keyIds,
                    proxyJumpId = if (o.has("proxyJumpId")) o.getString("proxyJumpId") else null,
                    useMosh = o.optBoolean("useMosh", false),
                    totpSecretId = if (o.has("totpSecretId")) o.getString("totpSecretId") else null,
                    startupCommands = startup,
                    envVars = envVars,
                    envSecretsPlain = envSecrets,
                )
            }

            val keys = mutableListOf<BackupKey>()
            val kArr = root.optJSONArray("keys") ?: JSONArray()
            for (i in 0 until kArr.length()) {
                val o = kArr.getJSONObject(i)
                keys += BackupKey(
                    id = o.getString("id"),
                    type = o.getString("type"),
                    privateKeyPkcs8Base64 = if (o.has("privateKeyBase64")) o.getString("privateKeyBase64") else null,
                    publicKeyOpenSsh = o.getString("publicKeyOpenSsh"),
                    fingerprintSha256 = o.getString("fingerprintSha256"),
                    comment = if (o.has("comment")) o.getString("comment") else null,
                    createdAt = o.getLong("createdAt"),
                )
            }

            val snippets = mutableListOf<BackupSnippet>()
            val snArr = root.optJSONArray("snippets") ?: JSONArray()
            for (i in 0 until snArr.length()) {
                val o = snArr.getJSONObject(i)
                snippets += BackupSnippet(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    command = o.getString("command"),
                    sortOrder = o.optInt("sortOrder", 0),
                )
            }

            val hosts = mutableListOf<BackupKnownHost>()
            val hArr = root.optJSONArray("knownHosts") ?: JSONArray()
            for (i in 0 until hArr.length()) {
                val o = hArr.getJSONObject(i)
                hosts += BackupKnownHost(
                    host = o.getString("host"),
                    port = o.getInt("port"),
                    fingerprintSha256 = o.getString("fingerprintSha256"),
                    addedAt = o.getLong("addedAt"),
                )
            }

            val fwds = mutableListOf<BackupPortForward>()
            val pArr = root.optJSONArray("portForwards") ?: JSONArray()
            for (i in 0 until pArr.length()) {
                val o = pArr.getJSONObject(i)
                fwds += BackupPortForward(
                    id = o.getString("id"),
                    serverId = o.getString("serverId"),
                    localPort = o.getInt("localPort"),
                    remoteHost = o.getString("remoteHost"),
                    remotePort = o.getInt("remotePort"),
                    enabled = o.optBoolean("enabled", true),
                    createdAt = o.optLong("createdAt", 0L),
                )
            }

            return BackupData(
                format = format,
                formatVersion = formatVersion,
                appVersion = appVersion,
                exportedAt = exportedAt,
                includesSecrets = includesSecrets,
                servers = servers,
                keys = keys,
                snippets = snippets,
                knownHosts = hosts,
                portForwards = fwds,
            )
        }
    }
}

data class BackupServer(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val tags: List<String>,
    val authType: String,
    val passwordPlain: String?,
    val keyIds: List<String>?,
    val proxyJumpId: String?,
    val useMosh: Boolean,
    val totpSecretId: String?,
    val startupCommands: List<String>,
    val envVars: Map<String, String>,
    val envSecretsPlain: Map<String, String>,
    val createdAt: Long = 0L,
    val lastUsedAt: Long? = null,
)

data class BackupKey(
    val id: String,
    val type: String,
    val privateKeyPkcs8Base64: String?,
    val publicKeyOpenSsh: String,
    val fingerprintSha256: String,
    val comment: String?,
    val createdAt: Long,
)

data class BackupSnippet(
    val id: String,
    val name: String,
    val command: String,
    val sortOrder: Int,
)

data class BackupKnownHost(
    val host: String,
    val port: Int,
    val fingerprintSha256: String,
    val addedAt: Long,
)

data class BackupPortForward(
    val id: String,
    val serverId: String,
    val localPort: Int,
    val remoteHost: String,
    val remotePort: Int,
    val enabled: Boolean,
    val createdAt: Long,
)
