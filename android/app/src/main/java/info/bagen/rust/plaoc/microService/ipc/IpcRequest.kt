package info.bagen.rust.plaoc.microService.ipc

data class IpcRequest(
    val req_id: Number = 0,
    val method: String = "",
    val url: String = "",
    val body: String = "",
    val headers: MutableMap<String, String> = mutableMapOf(),
    val type: Number = 0
)