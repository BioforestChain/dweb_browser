package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.helper.*
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri


abstract class Ipc {
    companion object {
        private var uid_acc = 1
        private var req_id_acc = 0;
    }

    val uid = uid_acc++

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

    abstract val remote: MicroModuleInfo

    fun asRemoteInstance() = if (remote is MicroModule) remote as MicroModule else null

    interface MicroModuleInfo {
        val mmid: Mmid
    }

    abstract val role: String

    override fun toString() = "#i$uid"

    suspend fun postMessage(message: IpcMessage) {
        if (this._closed) {
            return;
        }
        this._doPostMessage(message);
    }


    suspend fun postResponse(req_id: Int, response: Response) {
        postMessage(
            IpcResponse.fromResponse(
                req_id,
                response,
                this
            )
        )
    }

    protected val _messageSignal = Signal<IpcMessageArgs>();
    fun onMessage(cb: OnIpcMessage) = _messageSignal.listen(cb)

    abstract suspend fun _doPostMessage(data: IpcMessage): Unit;

    private val _requestSignal by lazy {
        Signal<IpcRequestMessageArgs>().also { signal ->
            _messageSignal.listen { args ->
                if (args.message is IpcRequest) {
                    signal.emit(IpcRequestMessageArgs(args.message, args.ipc));
                }
            }
        }
    }

    fun onRequest(cb: OnIpcRequestMessage) = _requestSignal.listen(cb)

    private val _eventSignal by lazy {
        Signal<IpcEventMessageArgs>().also { signal ->
            _messageSignal.listen { args ->
                if (args.message is IpcEvent) {
                    signal.emit(IpcEventMessageArgs(args.message, args.ipc));
                }
            }
        }
    }

    fun onEvent(cb: OnIpcEventMessage) = _eventSignal.listen(cb)


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

    suspend fun request(ipcRequest: IpcRequest): IpcResponse {
        this.postMessage(ipcRequest)
        val result = PromiseOut<IpcResponse>();
        this.onMessage { args ->
            if (args.message is IpcResponse && args.message.req_id == ipcRequest.req_id) {
                result.resolve(args.message)
                return@onMessage SIGNAL_CTOR.OFF
            } else {
            }
        }
        return result.waitPromise()
    }

    suspend fun request(request: Request) =
        this.request(IpcRequest.fromRequest(allocReqId(), request, this)).toResponse()

    fun allocReqId() = req_id_acc++;

}

