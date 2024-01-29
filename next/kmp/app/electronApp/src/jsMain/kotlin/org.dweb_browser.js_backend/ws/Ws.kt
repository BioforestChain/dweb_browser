package org.dweb_browser.js_backend.ws

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import node.buffer.Buffer
import node.http.createServer
import node.http.IncomingMessage
import node.http.ServerResponse
import node.net.Socket
import node.stream.Duplex
import node.url.parse
import org.dweb_browser.js_backend.http.HttpServer
import org.dweb_browser.js_backend.view_model.ViewModelSocket

typealias onUpgradeCallback = (req: IncomingMessage, socket: Socket, head: Buffer) -> Unit

/**
 * WS依赖HttpServer
 * - 共用一个port
 */
class WS private constructor(){
    val scope = CoroutineScope(Dispatchers.Default)
    val whenReady = CompletableDeferred<Unit>()
    init {
        scope.launch {
            HttpServer.deferredInstance.await().run {
                console.log("注册了 upgrade")
                getServer().on("upgrade"){req: IncomingMessage, socket: Socket, head: Buffer ->
                    onUpgradeCallbackList.forEach { cb ->cb(req, socket, head) }
                }
            }
        }
    }

    companion object {
        var deferredInstance = CompletableDeferred<WS>()
        fun createWS(): CompletableDeferred<WS>{
            return if(deferredInstance.isCompleted) {
                deferredInstance
            }else{
                deferredInstance.complete(WS())
                deferredInstance
            }
        }

        var onUpgradeCallbackList = mutableListOf<onUpgradeCallback>()
        fun onUpgrade(cb: onUpgradeCallback): () -> Unit{
            if(!deferredInstance.isCompleted) {
                deferredInstance.complete(WS())
            }
            onUpgradeCallbackList.add(cb)
            return {onUpgradeCallbackList.remove(cb)}
        }
    }
}


