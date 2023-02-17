package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.microService.helper.Callback
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.Signal
import info.bagen.rust.plaoc.microService.ipc.*
import org.http4k.routing.RoutingHttpHandler

abstract class NativeMicroModule(override val mmid: Mmid = "sys.dweb") : MicroModule() {
    private val _connectedIpcSet = mutableSetOf<Ipc>();
    override suspend fun _connect(from: MicroModule): NativeIpc {
        val channel = NativeMessageChannel<IpcMessage, IpcMessage>();
        val nativeIpc = NativeIpc(channel.port1, from, IPC_ROLE.SERVER, true);

        this._connectedIpcSet.add(nativeIpc);
        nativeIpc.onClose {
            this._connectedIpcSet.remove(nativeIpc);
        };

        this._connectSignal.emit(nativeIpc);
        return NativeIpc(channel.port2, this, IPC_ROLE.CLIENT, true);
    }


    /**
     * 内部程序与外部程序通讯的方法
     * TODO 这里应该是可以是多个
     */
    private val _connectSignal = Signal<NativeIpc>();

    /**
     * 给内部程序自己使用的 onConnect，外部与内部建立连接时使用
     * 因为 NativeMicroModule 的内部程序在这里编写代码，所以这里会提供 onConnect 方法
     * 如果时 JsMicroModule 这个 onConnect 就是写在 WebWorker 那边了
     */
    protected fun onConnect(cb: Callback<NativeIpc>) = _connectSignal.listen(cb);

    /** 在模块关停后，从自身构建的通讯通道都要关闭掉 */
    override fun afterShutdown() {
        super.afterShutdown();
        for (inner_ipc in this._connectedIpcSet) {
            inner_ipc.close();
        }
        _connectedIpcSet.clear();
    }

    protected abstract var routes: RoutingHttpHandler?

    /**
     * 实现一整套简易的路由响应规则
     */
    init {
        onConnect { clientIpc ->
            clientIpc.onRequest { args ->
                routes?.let { routes ->
                    routes(args.request.asRequest())
                } ?: return@onRequest
            }
        }
    }
}
