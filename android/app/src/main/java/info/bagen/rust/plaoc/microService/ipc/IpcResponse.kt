package info.bagen.rust.plaoc.microService.ipc

import com.google.gson.annotations.SerializedName
import info.bagen.rust.plaoc.microService.network.gson

data class IpcResponse(
    @SerializedName("req_id") val req_id: Number = 0,
    @SerializedName("statusCode") val statusCode: Number = 200,
    @SerializedName("body") val body: String = "",
    @SerializedName("headers") val headers: MutableMap<String, String> = mutableMapOf(),
    @SerializedName("type") val type: Int = 1
){
    fun fromJson(): String? {
        this.headers["Content-Type"] = "application/json"
        return gson.toJson(this)
    }

    fun fromText() {
        this.headers["Content-Type"] = "text/plain"
    }

    fun fromBinary() {
        this.headers["Content-Type"] = "application/octet-stream"
    }

    fun fromStream() {
        this.headers["Content-Type"] = "application/octet-stream"

    }
}
