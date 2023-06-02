package info.bagen.dwebbrowser.microService.core.ipc

import info.bagen.dwebbrowser.microService.helper.*
import info.bagen.dwebbrowser.microService.core.MicroModule
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import java.util.concurrent.atomic.AtomicInteger


abstract class Ipc {
    companion object {
        private var uid_acc = AtomicInteger(1)
        private var req_id_acc = AtomicInteger(0);
        private val ipcMessageCoroutineScope =
            CoroutineScope(CoroutineName("ipc-message") + ioAsyncExceptionHandler)
    }

    val uid = info.bagen.dwebbrowser.microService.core.ipc.Ipc.Companion.uid_acc.getAndAdd(1)

    /**
     * 是否支持 messagePack 协议传输：
     * 需要同时满足两个条件：通道支持直接传输二进制；通达支持 MessagePack 的编解码
     */
    open val supportMessagePack: Boolean = false

    /**
     * 是否支持 Protobuf 协议传输：
     * 需要同时满足两个条件：通道支持直接传输二进制；通达支持 Protobuf 的编解码
     */
    open val supportProtobuf: Boolean = false

    /**
     * 是否支持结构化内存协议传输：
     * 就是说不需要对数据手动序列化反序列化，可以直接传输内存对象
     */
    open val supportRaw: Boolean = false

    /** 是否支持 二进制 传输 */
    open val supportBinary: Boolean = false // get() = supportMessagePack || supportProtobuf

    abstract val remote: info.bagen.dwebbrowser.microService.core.ipc.Ipc.MicroModuleInfo

    fun asRemoteInstance() = if (remote is MicroModule) remote as MicroModule else null

    interface MicroModuleInfo {
        val mmid: Mmid
    }

    abstract val role: String

    override fun toString() = "#i$uid"

    suspend fun postMessage(message: info.bagen.dwebbrowser.microService.core.ipc.IpcMessage) {
        if (this._closed) {
            return;
        }
        this._doPostMessage(message);
    }


    suspend fun postResponse(req_id: Int, response: Response) {
        postMessage(
            info.bagen.dwebbrowser.microService.core.ipc.IpcResponse.Companion.fromResponse(
                req_id,
                response,
                this
            )
        )
    }

    protected val _messageSignal = Signal<info.bagen.dwebbrowser.microService.core.ipc.IpcMessageArgs>();
    fun onMessage(cb: info.bagen.dwebbrowser.microService.core.ipc.OnIpcMessage) = _messageSignal.listen(cb)

    /**
     * 强制触发消息传入，而不是依赖远端的 postMessage
     */
    suspend fun emitMessage(args: info.bagen.dwebbrowser.microService.core.ipc.IpcMessageArgs) = _messageSignal.emit(args)

    abstract suspend fun _doPostMessage(data: info.bagen.dwebbrowser.microService.core.ipc.IpcMessage): Unit;

    private val _requestSignal by lazy {
        Signal<info.bagen.dwebbrowser.microService.core.ipc.IpcRequestMessageArgs>().also { signal ->
            _messageSignal.listen { args ->
                if (args.message is info.bagen.dwebbrowser.microService.core.ipc.IpcRequest) {
                    info.bagen.dwebbrowser.microService.core.ipc.Ipc.Companion.ipcMessageCoroutineScope.launch {
                        signal.emit(
                            info.bagen.dwebbrowser.microService.core.ipc.IpcRequestMessageArgs(
                                args.message,
                                args.ipc
                            )
                        );
                    }
                }
            }
        }
    }

    fun onRequest(cb: info.bagen.dwebbrowser.microService.core.ipc.OnIpcRequestMessage) = _requestSignal.listen(cb)

    private val _responseSignal by lazy {
        Signal<info.bagen.dwebbrowser.microService.core.ipc.IpcResponseMessageArgs>().also { signal ->
            _messageSignal.listen { args ->
                if (args.message is info.bagen.dwebbrowser.microService.core.ipc.IpcResponse) {
                    info.bagen.dwebbrowser.microService.core.ipc.Ipc.Companion.ipcMessageCoroutineScope.launch {
                        signal.emit(
                            info.bagen.dwebbrowser.microService.core.ipc.IpcResponseMessageArgs(
                                args.message,
                                args.ipc
                            )
                        );
                    }
                }
            }
        }
    }

    fun onResponse(cb: info.bagen.dwebbrowser.microService.core.ipc.OnIpcResponseMessage) = _responseSignal.listen(cb)

    private val _streamSignal by lazy {
        val signal = Signal<info.bagen.dwebbrowser.microService.core.ipc.IpcStreamMessageArgs>()
        /// 这里建立起一个独立的顺序队列，目的是避免处理阻塞
        /// TODO 这里不应该使用 UNLIMITED，而是压力到一定程度方向发送限流的指令
        val streamChannel = Channel<info.bagen.dwebbrowser.microService.core.ipc.IpcStreamMessageArgs>(capacity = Channel.UNLIMITED)
        info.bagen.dwebbrowser.microService.core.ipc.Ipc.Companion.ipcMessageCoroutineScope.launch {
            for (message in streamChannel) {
                signal.emit(message);
            }
        }
        _messageSignal.listen { args ->
            if (args.message is info.bagen.dwebbrowser.microService.core.ipc.IpcStream) {
                streamChannel.trySend(
                    info.bagen.dwebbrowser.microService.core.ipc.IpcStreamMessageArgs(
                        args.message,
                        args.ipc
                    )
                )
            }
        }
        onClose {
            streamChannel.close();
        }
        signal
    }

    fun onStream(cb: info.bagen.dwebbrowser.microService.core.ipc.OnIpcStreamMessage) = _streamSignal.listen(cb)

    private val _eventSignal by lazy {
        Signal<info.bagen.dwebbrowser.microService.core.ipc.IpcEventMessageArgs>().also { signal ->
            _messageSignal.listen { args ->
                if (args.message is info.bagen.dwebbrowser.microService.core.ipc.IpcEvent) {
                    info.bagen.dwebbrowser.microService.core.ipc.Ipc.Companion.ipcMessageCoroutineScope.launch {
                        signal.emit(
                            info.bagen.dwebbrowser.microService.core.ipc.IpcEventMessageArgs(
                                args.message,
                                args.ipc
                            )
                        );
                    }
                }
            }
        }
    }

    fun onEvent(cb: info.bagen.dwebbrowser.microService.core.ipc.OnIpcEventMessage) = _eventSignal.listen(cb)


    abstract suspend fun _doClose(): Unit;

    private var _closed = false
    suspend fun close() {
        if (this._closed) {
            return;
        }
        this._closed = true;
        this._doClose();
        this._closeSignal.emit();
        this._closeSignal.clear();

        /// 关闭的时候会自动触发销毁
        this.destroy(false)
    }

    val isClosed get() = _closed

    private val _closeSignal = SimpleSignal();
    fun onClose(cb: SimpleCallback) = this._closeSignal.listen(cb)


    private val _destroySignal = SimpleSignal()
    fun onDestroy(cb: SimpleCallback) = this._destroySignal.listen(cb)

    private var _destroyed = false
    val isDestroy get() = _destroyed

    /**
     * 销毁实例
     */
    suspend fun destroy(close: Boolean = true) {
        if (_destroyed) {
            return
        }
        _destroyed = true
        if (close) {
            this.close()
        }
        this._destroySignal.emit()
        this._destroySignal.clear()
    }

    /**
     * 发送请求
     */
    suspend fun request(url: String) =
        request(Request(Method.GET, url))

    suspend fun request(url: Uri) =
        request(Request(Method.GET, url))

    private val _reqResMap by lazy {
        mutableMapOf<Int, PromiseOut<info.bagen.dwebbrowser.microService.core.ipc.IpcResponse>>().also { reqResMap ->
            onResponse { (response) ->
                val result = reqResMap.remove(response.req_id)
                    ?: throw Exception("no found response by req_id: ${response.req_id}")
                result.resolve(response)
            }
        }
    }

    suspend fun request(ipcRequest: info.bagen.dwebbrowser.microService.core.ipc.IpcRequest): info.bagen.dwebbrowser.microService.core.ipc.IpcResponse {
        val result = PromiseOut<info.bagen.dwebbrowser.microService.core.ipc.IpcResponse>();
        _reqResMap[ipcRequest.req_id] = result;
        this.postMessage(ipcRequest)
        return result.waitPromise()
    }

    suspend fun request(request: Request) =
        this.request(
            info.bagen.dwebbrowser.microService.core.ipc.IpcRequest.Companion.fromRequest(
                allocReqId(),
                request,
                this
            )
        ).toResponse()

    fun allocReqId() = info.bagen.dwebbrowser.microService.core.ipc.Ipc.Companion.req_id_acc.getAndAdd(1);
}

