package info.bagen.rust.plaoc.microService

class Connection {
}


class IpcRequest(method:String,
                 url:String,
                 body:String,
                 headers: Map<String, String>,
                 onResponse:(response:IpcResponse)->Void){

}

class IpcResponse(request:IpcRequest,
                  statusCode:Number,
                  body:String){

}
class Ipc {
    fun postMessage (request: IpcRequest) {

    }
    fun onMessage (request: IpcRequest){

    }
}