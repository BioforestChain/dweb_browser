package info.bagen.rust.plaoc.microService.core

import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.helper.runBlockingCatching
import info.bagen.rust.plaoc.microService.ipc.*
import org.http4k.core.*
import org.http4k.filter.ServerFilters
import org.http4k.lens.RequestContextKey
import org.http4k.routing.RoutingHttpHandler
import java.io.InputStream


abstract class NativeMicroModule(override val mmid: Mmid) : MicroModule() {

    companion object {
        init {
            connectAdapterManager.append { fromMM, toMM, reason ->
                if (toMM is NativeMicroModule) {
                    val channel = NativeMessageChannel<IpcMessage, IpcMessage>();
                    val toNativeIpc = NativeIpc(channel.port1, fromMM, IPC_ROLE.SERVER);
                    val fromNativeIpc = NativeIpc(channel.port2, toMM, IPC_ROLE.CLIENT);
                    fromMM.beConnect(fromNativeIpc, reason) // 通知发起连接者作为Client
                    toMM.beConnect(toNativeIpc, reason) // 通知接收者作为Server
                    return@append ConnectResult(fromNativeIpc, toNativeIpc) // 返回发起者的ipc
                } else null
            }
        }
    }

    var apiRouting: RoutingHttpHandler? = null


    private val requestContexts = RequestContexts()
    private val requestContextKey_ipc = RequestContextKey.required<Ipc>(requestContexts)
    private val ipcApiFilter = ServerFilters.InitialiseRequestContext(requestContexts)

    /**
     * 实现一整套简易的路由响应规则
     */
    init {
        onConnect { (clientIpc) ->
            clientIpc.onRequest { (ipcRequest) ->
                val routes = apiRouting ?: return@onRequest null;
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

    protected fun defineHandler(handler: suspend (request: Request) -> Any?) = { request: Request ->
        runBlockingCatching {
            when (val result = handler(request)) {
                is Response -> result
                is ByteArray -> Response(Status.OK).body(MemoryBody(result))
                is InputStream -> Response(Status.OK).body(result)
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

