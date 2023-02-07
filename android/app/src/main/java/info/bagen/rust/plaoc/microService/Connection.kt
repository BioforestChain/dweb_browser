package info.bagen.rust.plaoc.microService

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName


// 工具方法
val gson = Gson()

enum class IPC_DATA_TYPE(type:Number) {
    REQUEST(0),
    RESPONSE(1)
}

data class IpcRequest(
    val req_id:Number = 0,
    val method: String = "",
    val url: String= "",
    val body: String= "",
    val headers: MutableMap<String, String> =  mutableMapOf(),
    val type: Number = 0
)

data class IpcResponse(
    @SerializedName("req_id") val req_id:Number = 0,
    @SerializedName("statusCode") val statusCode: Number = 200,
    @SerializedName("body") val body: String= "",
    @SerializedName("headers") val headers: MutableMap<String, String> =  mutableMapOf(),
    @SerializedName("type") val type:Int  = 1
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

var ipc_uid_acc = 0
abstract class Ipc {
    abstract val supportMessagePack: Boolean

}




