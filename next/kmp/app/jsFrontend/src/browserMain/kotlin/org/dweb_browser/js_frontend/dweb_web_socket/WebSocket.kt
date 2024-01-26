package org.dweb_browser.js_frontend.dweb_web_socket


import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import org.w3c.dom.events.Event
import kotlin.js.JSON

typealias OnOpenedCallback = (e: Event) -> Unit
typealias OnErrorCallback = (e: Event) -> Unit
typealias OnCloseCallback = (e: Event) -> Unit
typealias OnMessageCallback = (e: MessageEvent) -> Unit

open class DwebWebSocket(
    val url: String
){
    val scope = CoroutineScope(Dispatchers.Default)
    val whenOpened = CompletableDeferred<Unit>()
    val whenColose = CompletableDeferred<Unit>()
    lateinit var socket: WebSocket;
    var readyState = WebSocket.CLOSED

    fun start(){
        console.log("发起了 upgrade 请求")
        val has = urlHasFrontedViewModelId()
        if(!has){
            console.error("url缺少frontend_view_module_id 查询参数:$url")
            return
        }
        socket = WebSocket(url)
        socket.onopen = {
            whenOpened.complete(Unit)
            readyState = WebSocket.OPEN
            onOpenedCallbackList.forEach { cb -> cb(it)}
            console.log("socket onopen")
        }
        socket.onerror = {
            console.error("onError")
            onErrorCallbackList.forEach { cb -> cb(it) }
        }
        socket.onmessage = {
//            console.log("onMessage", it)
            onMessageCallbackList.forEach { cb -> cb(it) }
        }

        socket.onclose = {
            whenColose.complete(Unit)
            readyState = WebSocket.CLOSED
            onCloseCallbackList.forEach{ cb -> cb(it) }
        }
    }

    // TODO: 检查 url 是否带有  frontend_view_module_id 的参数
    private fun urlHasFrontedViewModelId(): Boolean{
        return url.contains("frontend_view_module_id")
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

    private val onMessageCallbackList = mutableListOf<OnMessageCallback>()
    fun onMessage(cb: OnMessageCallback): () -> Unit {
        onMessageCallbackList.add(cb)
        return {
            onMessageCallbackList.remove(cb)
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

    fun send(data: String){
        scope.launch {
            whenOpened.await()
            console.log("向后端发送了数据： ", data)
            socket.send(data)
        }
    }
}