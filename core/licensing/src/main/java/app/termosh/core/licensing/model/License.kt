package app.termosh.core.licensing.model

import org.json.JSONArray
import org.json.JSONObject

data class License(
    val version: Int,
    val id: String,
    val recipientName: String,
    val devicePublicKeyBase64: String,
    val features: List<String>,
    val issuedAt: Long,
    val expiresAt: Long?,
    val nonce: String,
) {
    fun encodeCanonical(): String {
        val obj = JSONObject()
        obj.put("v", version)
        obj.put("id", id)
        obj.put("name", recipientName)
        obj.put("devicePublicKey", devicePublicKeyBase64)
        obj.put("features", JSONArray(features))
        obj.put("issuedAt", issuedAt)
        if (expiresAt != null) obj.put("expiresAt", expiresAt) else obj.put("expiresAt", JSONObject.NULL)
        obj.put("nonce", nonce)
        return obj.toString()
    }

    companion object {
        fun decode(s: String): License {
            val j = JSONObject(s)
            val features = mutableListOf<String>()
            val arr = j.getJSONArray("features")
            for (i in 0 until arr.length()) features += arr.getString(i)
            return License(
                version = j.getInt("v"),
                id = j.getString("id"),
                recipientName = j.getString("name"),
                devicePublicKeyBase64 = j.getString("devicePublicKey"),
                features = features,
                issuedAt = j.getLong("issuedAt"),
                expiresAt = if (j.isNull("expiresAt")) null else j.getLong("expiresAt"),
                nonce = j.getString("nonce"),
            )
        }
    }
}
