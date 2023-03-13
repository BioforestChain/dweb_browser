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

    class ResponseRegistry {
        companion object {
            val regMap = mutableMapOf<Class<Any>, (item: Any) -> Response>(
//                Pair(ByteArray::class.java, { Response(Status.OK).body(MemoryBody(it)) })
            )

            fun <T : Any> registryResponse(type: Class<T>, handler: (item: T) -> Response) {
                regMap[type as Class<Any>] = handler as (item: Any) -> Response
            }

            init {
                registryResponse(ByteArray::class.java) {
                    Response(Status.OK).body(MemoryBody(it))
                }
                registryResponse(InputStream::class.java) {
                    Response(Status.OK).body(it)
                }
            }

            fun <T : Any> registryJsonAble(type: Class<T>, handler: (item: T) -> Any) {
                registryResponse(type) {
                    asJson(handler(it))
                }
            }

            fun handle(result: Any): Response {
                val javaClass = result.javaClass
                return when (val handler = regMap[javaClass]) {
                    null -> {
                        var superJavaClass = javaClass.superclass
                        while (superJavaClass != null) {
                            // 尝试寻找继承关系
                            when (val handler = regMap[superJavaClass!!]) {
                                null -> superJavaClass = superJavaClass!!.superclass
                                else -> return handler(result)
                            }
                        }
                        // 否则默认当成JSON来返回
                        return asJson(result)
                    }
                    // 如果有注册处理函数，那么交给处理函数进行处理
                    else -> handler(result)
                }

            }

            inline fun asJson(result: Any) = Response(Status.OK).body(gson.toJson(result))
                .header("Content-Type", "application/json")

        }
    }

    protected fun defineHandler(handler: suspend (request: Request) -> Any?) = { request: Request ->
        runBlockingCatching {
            when (val result = handler(request)) {
                null, Unit -> {
                    Response(Status.OK)
                }
                is Response -> result
                is ByteArray -> Response(Status.OK).body(MemoryBody(result))
                is InputStream -> Response(Status.OK).body(result)
                else -> {
                    // 如果有注册处理函数，那么交给处理函数进行处理
                    ResponseRegistry.handle(result)
                }
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

