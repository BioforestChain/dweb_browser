package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.microService.helper.Callback
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.Signal
import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.ipc.*
import kotlinx.coroutines.runBlocking
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
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

    var apiRouting: RoutingHttpHandler? = null

    /**
     * 实现一整套简易的路由响应规则
     */
    init {
        onConnect { clientIpc ->
            clientIpc.onRequest { args ->
                apiRouting?.let { routes ->
                    routes(args.request.asRequest())
                }
            }
        }
    }

    protected fun apiHandler(handler: suspend (request: Request) -> Any?) = { request: Request ->
        runBlocking {
            try {
                Response(Status.OK).json(handler(request))
            } catch (ex: Exception) {
                Response(Status.INTERNAL_SERVER_ERROR).body(ex.message ?: "Unknown Error")
            }
        }
    }
}


inline fun Response.json(src: Any?) =
    this.body(gson.toJson(src)).header("Content-Type", "application/json")

