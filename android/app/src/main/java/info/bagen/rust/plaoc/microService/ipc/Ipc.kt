package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.MicroModule
import info.bagen.rust.plaoc.microService.helper.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import okhttp3.internal.notify
import okhttp3.internal.wait
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import java.io.InputStream

var ipc_uid_acc = 0

abstract class Ipc {
    /**
     * 是否支持 messagePack 协议传输：
     * 需要同时满足两个条件：通道支持直接传输二进制；通达支持 MessagePack 的编解码
     */
    val supportMessagePack: Boolean = false

    /**
     * 是否支持 Protobuf 协议传输：
     * 需要同时满足两个条件：通道支持直接传输二进制；通达支持 Protobuf 的编解码
     */
    val supportProtobuf: Boolean = false

    /** 是否支持 二进制 传输 */
    val supportBinary get() = supportMessagePack || supportProtobuf

    abstract val remote: MicroModule
    abstract val role: IPC_ROLE
    val uid = ipc_uid_acc++

    suspend fun postMessage(message: IpcMessage) {
        if (this._closed) {
            return;
        }
        this._doPostMessage(message);
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

    abstract suspend fun _doClose(): Unit;

    private var _closed = false
    suspend fun close() {
        if (this._closed) {
            return;
        }
        this._closed = true;
        this._doClose();
        this._closeSignal.emit();
    }

    private val _closeSignal = SimpleSignal();
    fun onClose(cb: SimpleCallback) = this._closeSignal.listen(cb)

    /**
     * 发送请求
     */
    suspend fun request(url: String) =
        request(Request(Method.GET, url))

    suspend fun request(url: Uri) =
        request(Request(Method.GET, url))

    suspend fun request(request: Request): IpcResponse {
        val req_id = allocReqId()
        this.postMessage(IpcRequest.fromRequest(req_id, request, this))
        val result = Channel<IpcResponse>();
        this.onMessage { args ->
            if (args.message is IpcResponse && args.message.req_id == req_id) {
                runBlocking {
                    result.send(args.message)
                }
            }
        }
        return result.receive()
    }

    private var _req_id_acc: Int = 0;
    fun allocReqId() = _req_id_acc++;

    suspend fun responseBy(byIpc:Ipc, myRequest: IpcRequest){
        postMessage(
            IpcResponse.fromResponse(
                myRequest.req_id,
                byIpc.request(myRequest.asRequest()).asResponse(),
                byIpc
            )
        )
    }
}

