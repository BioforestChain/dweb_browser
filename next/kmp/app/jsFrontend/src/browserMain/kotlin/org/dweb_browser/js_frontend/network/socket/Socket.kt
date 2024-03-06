package org.dweb_browser.js_frontend.network.socket

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dweb_browser.js_common.network.socket.SyncState
import org.w3c.dom.WebSocket
import org.w3c.dom.events.Event

typealias OnMessageCallback = (str: String) -> Boolean
typealias OnOpenedCallback = (e: Event) -> Unit
typealias OnErrorCallback = (e: Event) -> Unit
typealias OnCloseCallback = (e: Event) -> Unit

open class Socket(
    val url: String
){
    val scope = CoroutineScope(Dispatchers.Default)
    val whenOpened = CompletableDeferred<Unit>()
    val whenColose = CompletableDeferred<Unit>()
    lateinit var socket: WebSocket;
    var readyState = WebSocket.CLOSED


    fun start(){
        console.log("发起了 upgrade 请求")
        socket = WebSocket(url)
        socket.onopen = {
            whenOpened.complete(Unit)
            readyState = WebSocket.OPEN
            onOpenedCallbackList.forEach { cb -> cb(it)}
            console.log("socket onopen")
        }
        socket.onerror = {
            console.error("onError", it)
            onErrorCallbackList.forEach { cb -> cb(it) }
        }
        socket.onmessage = {
            val data = it.data
            require(data is String)
            for(cb in onMessageCallbackList){
                if(cb(data)) break
            }
        }

        socket.onclose = {
            whenColose.complete(Unit)
            readyState = WebSocket.CLOSED
            onCloseCallbackList.forEach{ cb -> cb(it) }
        }
    }

    private val onMessageCallbackList = mutableListOf<OnMessageCallback>()
    fun onMessage(cb: OnMessageCallback): () -> Unit{
        onMessageCallbackList.add(cb)
        return {onMessageCallbackList.remove(cb)}
    }

    private val onOpenedCallbackList = mutableListOf<OnOpenedCallback>()
    fun onOpened(cb: OnOpenedCallback): () -> Unit{
        onOpenedCallbackList.add(cb)
        return {
            onOpenedCallbackList.remove(cb)
        }
    }

    private val onErrorCallbackList = mutableListOf<OnErrorCallback>()
    fun onError(cb: OnErrorCallback): () -> Unit{
        onErrorCallbackList.add(cb)
        return {
            onErrorCallbackList.remove(cb)
        }
    }

    private val onCloseCallbackList = mutableListOf<OnCloseCallback>()
    fun onClose(cb: OnCloseCallback): () -> Unit{
        onCloseCallbackList.add(cb)
        return {
            onCloseCallbackList.remove(cb)
        }
    }

    fun close(){
        socket.close()
    }


    fun syncToServer(value: String){
        scope.launch {
            console.log("发送给了 服务端", value)
            socket.send(value)
        }
    }
}