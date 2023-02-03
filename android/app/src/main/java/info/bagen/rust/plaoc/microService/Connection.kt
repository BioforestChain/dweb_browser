package info.bagen.rust.plaoc.microService

enum class IPC_DATA_TYPE {
    REQUEST,
    RESPONSE
}

class IpcRequest(method:String,
                 url:String,
                 body:String,
                 headers: Map<String, String>,
                 onResponse:(response:IpcResponse)->Void){
    private val type = IPC_DATA_TYPE.REQUEST
}

class IpcResponse(request:IpcRequest,
                  statusCode:Number,
                  body:String){
    private val type = IPC_DATA_TYPE.RESPONSE

}

var ipc_uid_acc = 0;
 abstract class Ipc {
     val uid = ipc_uid_acc++;
    abstract fun postMessage(data: IpcRequest): Void
    abstract fun onMessage(
    cb: (message: IpcRequest )-> Void
    ): () -> Boolean;
    abstract fun close(): Void;
    abstract fun onClose(cb: () -> Void): () -> Boolean;
}