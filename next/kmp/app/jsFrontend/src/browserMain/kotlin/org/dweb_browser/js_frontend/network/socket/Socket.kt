package org.dweb_browser.js_frontend.network.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.w3c.dom.WebSocket
import org.w3c.dom.events.Event

open class Socket(
    val url: String
){
    val scope = CoroutineScope(Dispatchers.Default)
    lateinit var socket: WebSocket;
    var readyState: dynamic = WebSocket.CLOSED
    val messageFlow = MutableSharedFlow<String>(
        replay = 1, extraBufferCapacity =  0, onBufferOverflow = BufferOverflow.DROP_LATEST
    )
    val openFlow = MutableSharedFlow<Event>(
        replay = 1, extraBufferCapacity =  0, onBufferOverflow = BufferOverflow.DROP_LATEST
    )

    val errorFlow = MutableSharedFlow<Event>(
        replay = 1, extraBufferCapacity =  0, onBufferOverflow = BufferOverflow.DROP_LATEST
    )
    val closeFlow = MutableSharedFlow<Event>(
        replay = 1, extraBufferCapacity =  0, onBufferOverflow = BufferOverflow.DROP_LATEST
    )

    val syncToServerFlow = MutableSharedFlow<String>(
        replay = 1, extraBufferCapacity =  0, onBufferOverflow = BufferOverflow.DROP_LATEST
    )

    val closeJob = Job()

    fun start(){
        socket = WebSocket(url)
        socket.onopen = {
            readyState = WebSocket.OPEN;
            scope.launch {openFlow.emit(it) }
        }
        socket.onerror = {
            scope.launch { errorFlow.emit(it) }
        }
        socket.onmessage = {
            val data = it.data
            require(data is String)
            scope.launch { messageFlow.emit(data) }
        }

        socket.onclose = {
            closeJob.cancel()
            readyState = WebSocket.CLOSED
            scope.launch { closeFlow.emit(it) }
        }
    }

    fun close(){
        socket.close()
    }

    fun syncToServer(value: String){
        scope.launch {
            socket.send(value)
        }
    }

    init{
        val job = Job(closeJob)
        CoroutineScope(Dispatchers.Default + job).launch {
            syncToServerFlow.collect{
                socket.send(it)
            }
        }
    }
}