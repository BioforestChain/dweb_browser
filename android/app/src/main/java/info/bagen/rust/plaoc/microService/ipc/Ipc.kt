package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.MicroModule
import info.bagen.rust.plaoc.microService.helper.*

var ipc_uid_acc = 0

abstract class Ipc {
    abstract val supportMessagePack: Boolean
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
    fun onMessageWithOff(cb: CallbackWithOff<IpcMessageArgs>) = _messageSignal.listenWithOff(cb)

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

    abstract fun _doClose(): Unit;


    private var _closed = false
    fun close() {
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
    fun request()
    {

    }
}
