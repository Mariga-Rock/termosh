package app.termosh.core.licensing.model

import org.json.JSONObject

data class DeviceRequest(
    val uuid: String,
    val publicKeyBase64: String,
    val label: String,
) {
    fun encode(): String =
        JSONObject()
            .put("uuid", uuid)
            .put("pubkey", publicKeyBase64)
            .put("label", label)
            .toString()

    companion object {
        fun decode(s: String): DeviceRequest {
            val j = JSONObject(s)
            return DeviceRequest(
                uuid = j.getString("uuid"),
                publicKeyBase64 = j.getString("pubkey"),
                label = j.optString("label", ""),
            )
        }
    }
}
