package info.bagen.rust.plaoc.microService.core

import info.bagen.rust.plaoc.microService.helper.*
import info.bagen.rust.plaoc.microService.ipc.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.http4k.core.*
import org.http4k.filter.ServerFilters
import org.http4k.lens.RequestContextKey
import org.http4k.routing.RoutingHttpHandler

abstract class NativeMicroModule(override val mmid: Mmid) : MicroModule() {
    private val _connectedIpcSet = mutableSetOf<Ipc>();
    override suspend fun _connect(from: MicroModule): NativeIpc {
        val channel = NativeMessageChannel<IpcMessage, IpcMessage>();
        val innerNativeIpc = NativeIpc(channel.port1, from, IPC_ROLE.SERVER);
        val outerNativeIpc = NativeIpc(channel.port2, this, IPC_ROLE.CLIENT);

        this._connectedIpcSet.add(innerNativeIpc);
        innerNativeIpc.onClose {
            this._connectedIpcSet.remove(innerNativeIpc);
        };


        this._connectSignal.emit(innerNativeIpc);
        return outerNativeIpc
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
    override suspend fun afterShutdown() {
        super.afterShutdown();
        for (inner_ipc in this._connectedIpcSet) {
            inner_ipc.close();
        }
        _connectedIpcSet.clear();
    }

    var apiRouting: RoutingHttpHandler? = null


    private val requestContexts = RequestContexts()
    private val requestContextKey_ipc = RequestContextKey.required<Ipc>(requestContexts)
    private val ipcApiFilter = ServerFilters.InitialiseRequestContext(requestContexts)

    /**
     * 实现一整套简易的路由响应规则
     */
    init {
        onConnect { clientIpc ->
            clientIpc.onRequest { (ipcRequest) ->
                val routes = apiRouting ?: return@onRequest null;
                GlobalScope.launch {
                    val routesWithContext = routes.withFilter(ipcApiFilter.then(Filter { next ->
                        { next(it.with(requestContextKey_ipc of clientIpc)) }
                    }));
                    printdebugln("fetch", "NMM/Handler", ipcRequest.url)
                    val request = ipcRequest.toRequest()
                    val response = routesWithContext(request)
                    clientIpc.postMessage(
                        IpcResponse.fromResponse(
                            ipcRequest.req_id, response, clientIpc
                        )
                    )
                }
            }
        }
    }

    protected fun defineHandler(handler: suspend (request: Request) -> Any?) = { request: Request ->
        runBlockingCatching {
            when (val result = handler(request)) {
                is Response -> result
                else -> Response(Status.OK).body(gson.toJson(result))
                    .header("Content-Type", "application/json")
            }
        }.getOrElse { ex ->
            printdebugln("fetch", "NMM/Error", request.uri, ex)
            Response(Status.INTERNAL_SERVER_ERROR).body(
                """
                    <p>${request.uri}</p>
                    <pre>${ex.message ?: "Unknown Error"}</pre>    
                    """.trimIndent()

            )
        }

    }

    protected fun defineHandler(handler: suspend (request: Request, ipc: Ipc) -> Any?) =
        defineHandler { request ->
            handler(request, requestContextKey_ipc(request))
        }
}

