package org.dweb_browser.js_frontend.view_model


import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.WebSocket
import org.w3c.dom.events.Event

typealias OnOpenedCallback = (e: Event) -> Unit
typealias OnErrorCallback = (e: Event) -> Unit
typealias OnCloseCallback = (e: Event) -> Unit
typealias OnSyncFromServerCallback = (key: String, value: String) -> Unit

open class ViewModelSocket(
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
            console.error("onError")
            onErrorCallbackList.forEach { cb -> cb(it) }
        }
        socket.onmessage = {
            val data = it.data
            require(data is String)
            val syncData = Json.decodeFromString<SyncData>(data)
            onSyncFromServerCallbackList.forEach { cb -> cb(syncData.key, syncData.value) }
        }

        socket.onclose = {
            whenColose.complete(Unit)
            readyState = WebSocket.CLOSED
            onCloseCallbackList.forEach{ cb -> cb(it) }
        }
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

    private val onSyncFromServerCallbackList = mutableListOf<OnSyncFromServerCallback>()
    fun onSyncFromServer(cb: OnSyncFromServerCallback): () -> Unit {
        onSyncFromServerCallbackList.add(cb)
        return {
            onSyncFromServerCallbackList.remove(cb)
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


    fun syncToServer(key: String, value: String){
        scope.launch {
            whenOpened.await()
            val syncData = SyncData(key, value)
            val jsonSyncData = Json.encodeToString<SyncData>(syncData)
            socket.send(jsonSyncData)
        }
    }
}

@Serializable
data class SyncData(
    @JsName("key")
    val key: String,
    @JsName("value")
    val value: String
)