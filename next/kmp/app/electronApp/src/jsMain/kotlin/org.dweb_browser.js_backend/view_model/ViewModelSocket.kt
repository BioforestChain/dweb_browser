package org.dweb_browser.js_backend.view_model

import js.array.push
import js.core.BigInt
import js.typedarrays.Uint8Array
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import node.buffer.Buffer
import node.buffer.BufferEncoding
import node.crypto.BinaryToTextEncoding
import node.crypto.createHash
import node.net.Socket
import node.net.SocketEvent
import kotlin.experimental.xor
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * @param arg {Array<dynamic>}
 * - arr[0] 是被改变状态的key
 * - arr[1] 是被改变状态的value
 */
typealias OnDataCallback = (key: String, value: dynamic) -> Unit
typealias OnConnectCallback = () -> Unit
typealias OnCloseCallback = (hadError: Boolean) -> Unit
typealias OnEndCallback = () -> Unit
typealias OnErrorCallback = (err: Throwable) -> Unit
typealias EncodeValueToString = (key: String, value: dynamic) -> String
typealias DecodeValueFromString = (key: String, value: String) -> dynamic

/**
 * 提供前后端同步的功能
 */
class ViewModelSocket(
    val socket: Socket,
    val secWebsocketKey: String,
    val encodeValueToString: EncodeValueToString, /**编码value的方法*/
    val decodeValueFromString: DecodeValueFromString, /** 解码value的方法*/
){
    val scope = CoroutineScope(Dispatchers.Default)
    // 不要把_onDataCBList等变量移动到init的后面
    // 否则其他地方调用onData等方法这些变量 === undefined
    private val _onDataCBList = mutableListOf<OnDataCallback>()
    private val _onErrorCallbackList = mutableListOf<OnErrorCallback>()
    private val _onEndCallbackList = mutableListOf<OnEndCallback>()
    private val _onCloseCallbackList = mutableListOf<OnCloseCallback>()
    private val _onConnectCallbackList = mutableListOf<OnConnectCallback>()
    private val _realDataFlow = MutableSharedFlow<String>()
    init {
        socket.setKeepAlive(true)
        socket.setNoDelay()
        socket.write(_createSocketResMsg(secWebsocketKey))
        socket.on(SocketEvent.CONNECT, ::_onConnectCallback)
        socket.on(SocketEvent.CLOSE, ::_onCloseCallback)
        socket.on(SocketEvent.END, ::_onEndCallback)
        socket.on(SocketEvent.ERROR, ::_onErrorCallback)
        socket.on(SocketEvent.DATA, ::_onDataCallback)
        scope.launch {
            _realDataFlow.collect{
                _onDataCBList.forEach { cb ->
                    val syncData = Json.decodeFromString<SyncData>(it)
                    val value = decodeValueFromString(syncData.key, syncData.value)
                    cb(syncData.key, value)
                }
            }
        }
    }

    private fun _onConnectCallback(){
        console.log("socket on connect")
        _onConnectCallbackList.forEach { cb -> cb() }
    }

    private fun _onCloseCallback(hadError: Boolean){
        console.log("socket on close")
        _onCloseCallbackList.forEach { cb -> cb(hadError) }
    }

    private fun _onEndCallback(){
        console.log("socket on end")
        socket.end()
        _onEndCallbackList.forEach { cb -> cb() }
    }


    private fun _onErrorCallback(err: Throwable){
        console.error("socket on error", err)
        _onErrorCallbackList.forEach { cb -> cb(err) }
    }

    private fun _onDataCallback(data: Buffer) {
        _parseData(this,data)
    }

    fun bufToString(buf: Buffer, opcode: Int){
        when (opcode) {
            OPCODES.TEXT.value -> {
                val str = buf.toString(BufferEncoding.utf8)
                scope.launch {
                    _realDataFlow.emit(str)
                }
            }
            OPCODES.CLOSE.value -> {
                // 不需要处理
            }
            else -> {
                console.error("还有没有处理的消息类型 opcode: $opcode")
            }
        }
    }

    fun onConnect(cb: OnConnectCallback): () -> Unit{
        _onConnectCallbackList.add(cb)
        return {
            _onConnectCallbackList.remove(cb)
        }
    }
    fun onClose(cb: OnCloseCallback): () -> Unit{
        _onCloseCallbackList.add(cb)
        return {
            _onCloseCallbackList.remove(cb)
        }
    }
    fun onEnd(cb: OnEndCallback): () -> Unit{
        _onEndCallbackList.add(cb)
        return {_onEndCallbackList.remove(cb)}
    }

    fun onError(cb: OnErrorCallback): () -> Unit{
        _onErrorCallbackList.add(cb)
        return {_onErrorCallbackList.remove(cb)}
    }

    /**
     * 添加接收到数据的回调
     */
    fun onData(cb: OnDataCallback): () -> Unit{
        _onDataCBList.add(cb)
        return { _onDataCBList.remove(cb)}
    }

    /**
     * 向UI发送数据
     * @param arg {String}
     * - arg参数的规范：
     *  - 必须是一个数组类型的JsonString
     *  - arr[0]表示的是viewModel的key
     *  - arr[1]表示的是ViewModel的value
     */
    fun write(key: dynamic, value: dynamic): Unit{
        val valueString = when(key){
            "syncDataToUiState" -> value
            else -> encodeValueToString(key, value)
        }
        val syncData = SyncData(key.toString(), valueString)
        val jsonString = Json.encodeToString<SyncData>(syncData)
        socket.write(_encodeDataFrame(jsonString))
    }
}

/**
 *
 */
private fun _createSocketResMsg(secWebsocketKey: String): String{
    val guid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
    val value = createHash("sha1").update("${secWebsocketKey}${guid}").digest(
        BinaryToTextEncoding.base64
    )
    return listOf(
        "HTTP/1.1 101 Switching Protocols",
        "Upgrade: websocket",
        "Connection: Upgrade",
        "Sec-WebSocket-Accept:${value}",
        "",
        ""
    ).joinToString("\r\n")
}

enum class OPCODES(val key: String, val value: Int) {
    CONTINUE("CONTINUE", 0), TEXT("TEXT", 1), BINARY("BINARY", 2), CLOSE(
        "CLOSE",
        8
    ),
    PING("PING", 9), PONG("PONG", 10),
};

private fun _parseData(viewModelSocket: ViewModelSocket, data: Buffer) {
    val byte1 = data.readUInt8(0)
    val opcode = byte1.toInt() and 0x0f
    val byte2 = data.readUInt8(1);
    val str2 = byte2.toLong().toString(2);
    val mask: Char = str2[0];
    var curByteIndex = 2;
    when(val payloadLength = str2.substring(1).toInt(2)){
        126 -> {
            val len = data.readUInt16BE(2)
            curByteIndex += 2
            _parseDataCore<Double>(viewModelSocket, mask, data, curByteIndex, len, opcode)
        }
        127 -> {
            val len = data.readBigUInt64BE(2);
            curByteIndex += 8;
            _parseDataCore<BigInt>(viewModelSocket, mask, data, curByteIndex, len, opcode)
        }
        else -> {
            _parseDataCore<Int>(viewModelSocket, mask, data, curByteIndex, payloadLength, opcode)
        }
    }
}

private fun<PayloadLengthType> _parseDataCore(
    viewModelSocket: ViewModelSocket,
    mask: Char? = null,
    data: Buffer,
    curByteIndex: Int,
    payloadLength: PayloadLengthType,
    opcode: Int
){
    if(mask == null) return;
    val maskKey = data.slice(curByteIndex, curByteIndex + 4);
    val payloadData = when (payloadLength) {
        is BigInt -> {
            data.slice(curByteIndex + 4, curByteIndex + 4 + payloadLength);
        }

        is Double -> {
            data.slice(curByteIndex + 4, curByteIndex + 4 + payloadLength);
        }

        is Int -> {
            data.slice(curByteIndex + 4, curByteIndex + 4 + payloadLength);
        }

        else -> {
            throw(Throwable("""
                        payloadLength 是非法的类型
                        payloadLength : $payloadLength
                        at _parseDataCore
                        at Ws.kt
                    """.trimIndent()))
        }
    }
    val buf = _handleMask(maskKey, payloadData)
    viewModelSocket.bufToString(buf, opcode)
    val residue = when (payloadLength) {
        is BigInt -> {
            data.slice(curByteIndex + 4 + payloadLength)
        }

        is Double -> {
            data.slice(curByteIndex + 4 + payloadLength)
        }

        is Int -> {
            data.slice(curByteIndex + 4 + payloadLength)
        }

        else -> {
            throw(Throwable("""
                        payloadLength 是非法的类型
                        payloadLength : $payloadLength
                        at _parseDataCore
                        at Ws.kt
                    """.trimIndent()))
        }
    }
    if (residue.length != 0) _parseData(viewModelSocket, Buffer.from(residue))
}

private fun _handleMask(
    maskBytes: Uint8Array, payloadData: Uint8Array
): Buffer {
    val payload = Buffer.alloc(payloadData.length, "", BufferEncoding.utf8);
    for (i in 0..<payloadData.length) {
        payload[i] = maskBytes[i % 4] xor payloadData[i];
    }
    return payload;
}

private operator fun Number.plus(payloadLength: BigInt): Int {
    return this + payloadLength
}

private fun _encodeDataFrame(
    data: String,
): Buffer {
    // 获取第一位
    val bufferArr = arrayOf<Number>()
    val i = 0;
    val payloadData = Buffer.from(data); // 放到buffer
    val payloadLength = payloadData.length;
    val fin = 1 shl 7; // 转为2进制
    bufferArr.push(fin + 1); // 第一个字节拼好
    // 不是特殊长度直接使用 不用掩码
    if (payloadLength < 126) bufferArr.push(payloadLength);
    else if (payloadLength < 0x10000) bufferArr.push(126, (payloadLength and 0xFF00) shr 8, payloadLength and 0xFF);
    else bufferArr.push(
        127, 0, 0, 0, 0, //8字节数据，前4字节一般没用留空
        (payloadLength.toLong() and 0xFF000000) shr 24,
        (payloadLength and 0xFF0000) shr 16,
        (payloadLength and 0xFF00) shr 8,
        payloadLength and 0xFF
    );
    //返回头部分和数据部分的合并缓冲区
    val a = arrayOf(Buffer.from(bufferArr), payloadData)
    return Buffer.concat(a);
};

@Serializable
data class SyncData(
    @JsName("key")
    val key: String,
    @JsName("value")
    val value: String
)