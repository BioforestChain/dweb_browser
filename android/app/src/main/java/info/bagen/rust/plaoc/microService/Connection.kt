package info.bagen.rust.plaoc.microService

import com.google.gson.annotations.SerializedName

enum class IPC_DATA_TYPE(type:Number) {
    REQUEST(0),
    RESPONSE(1)
}

data class IpcRequest(
    val req_id:Number = 0,
    val method: String = "",
    val url: String= "",
    val body: String= "",
    val headers: Map<String, String> =  mapOf(),
    val type: Number = 0
)

data class IpcResponse(
    @SerializedName("req_id") val req_id:Number = 0,
    @SerializedName("statusCode") val statusCode: Number = 200,
    @SerializedName("body") val body: String= "",
    @SerializedName("headers") val headers: Map<String, String> =  mapOf(),
    @SerializedName("type") val type:Int  = 1
)

var ipc_uid_acc = 0
abstract class Ipc {
    abstract val supportMessagePack: Boolean

}




