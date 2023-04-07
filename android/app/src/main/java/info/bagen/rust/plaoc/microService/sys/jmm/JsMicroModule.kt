package info.bagen.rust.plaoc.microService.sys.jmm

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.ConnectResult
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.core.connectAdapterManager
import info.bagen.rust.plaoc.microService.helper.*
import info.bagen.rust.plaoc.microService.ipc.IpcResponse
import info.bagen.rust.plaoc.microService.ipc.ReadableStreamIpc
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.Native2JsIpc
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.http4k.core.*
import java.util.*

fun debugJMM(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("jmm", tag, msg, err)

open class JsMicroModule(val metadata: JmmMetadata) : MicroModule() {
    companion object {
        init {
            connectAdapterManager.append { fromMM, toMM, reason ->
                if (toMM is JsMicroModule) {
                    val pid = toMM.processId ?: throw Exception("JMM:${toMM.mmid} no ready");
                    /**
                     * 向js模块发起连接
                     */
                    val portId = toMM.nativeFetch(
                        Uri.of("file://js.sys.dweb/create-ipc").query("process_id", pid)
                            .query("mmid", fromMM.mmid)
                    ).int()
                    val originIpc = Native2JsIpc(portId, toMM).also {
                        // 同样要被生命周期管理销毁
                        toMM.beConnect(it, reason)
                    }
                    return@append ConnectResult(originIpc, null)
                } else null
            }

        }
    }

    override val mmid get() = metadata.id

    /**
     * 和 dweb 的 port 一样，pid 是我们自己定义的，它跟我们的 mmid 关联在一起
     * 所以不会和其它程序所使用的 pid 冲突
     */
    private var processId: String? = null
    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        debugJMM("bootstrap...", "$mmid/$metadata")
        val pid = ByteArray(8).also { Random().nextBytes(it) }.toBase64Url()
        processId = pid
        val streamIpc = ReadableStreamIpc(this, "code-server")
        streamIpc.onRequest { (request, ipc) ->
            val response = if (request.uri.path.endsWith("/")) {
                Response(Status.FORBIDDEN)
            } else {
                nativeFetch(metadata.server.root + request.uri.path)
            }

            ipc.postMessage(IpcResponse.fromResponse(request.req_id, response, ipc))
        }
        streamIpc.bindIncomeStream(
            nativeFetch(
                Request(
                    Method.POST,
                    Uri.of("file://js.sys.dweb/create-process")
                        .query("entry", metadata.server.entry).query("process_id", pid)
                ).body(streamIpc.stream)
            ).stream()
        )
        // 监听关闭事件
        closeJsProcessSignal.listen { streamIpc.close() }

        /**
         * 拿到与js.sys.dweb模块的直连通道，它会将 Worker 中的数据带出来
         */
        val (jsIpc) = bootstrapContext.dns.connect("js.sys.dweb")
        /**
         * 这里 jmm 的对于 request 的默认处理方式是将这些请求直接代理转发出去
         * TODO 跟 dns 要 jmmMetadata 信息然后进行路由限制 eg: jmmMetadata.permissions.contains(ipcRequest.uri.host) // ["camera.sys.dweb"]
         */
        jsIpc.onRequest { (ipcRequest, ipc) ->
            val request = ipcRequest.toRequest()
            kotlin.runCatching {
                // 转发请求
                val response = nativeFetch(request);
                val ipcResponse = IpcResponse.fromResponse(ipcRequest.req_id, response, ipc)
                ipc.postMessage(ipcResponse)
            }.onFailure {
                ipc.postMessage(
                    IpcResponse.fromText(
                        ipcRequest.req_id, 500, text = it.message ?: "", ipc = ipc
                    )
                )
            }
        }

        /**
         * 收到 Worker 的事件，如果是指令，执行一些特定的操作
         */
        jsIpc.onEvent { (ipcEvent) ->
            /**
             * 收到要与其它模块进行ipc连接的指令
             */
            if (ipcEvent.name == "dns/connect") {
                GlobalScope.launch(ioAsyncExceptionHandler) {
                    data class DnsConnectEvent(val mmid: Mmid)

                    val event = gson.fromJson(ipcEvent.text, DnsConnectEvent::class.java)
                    /**
                     * 模块之间的ipc是单例模式，所以我们必须拿到这个单例，再去做消息转发
                     * 但可以优化的点在于：TODO 我们应该将两个连接的协议进行交集，得到最小通讯协议，然后两个通道就能直接通讯raw数据，而不需要在转发的时候再进行一次编码解码
                     *
                     * 此外这里允许js多次建立ipc连接，因为可能存在多个js线程，它们是共享这个单例ipc的
                     */
                    /**
                     * 向目标模块发起连接
                     */
                    val (targetIpc) = bootstrapContext.dns.connect(event.mmid)
                    /**
                     * 向js模块发起连接
                     */
                    val portId = nativeFetch(
                        Uri.of("file://js.sys.dweb/create-ipc").query("process_id", pid)
                            .query("mmid", event.mmid)
                    ).int()
                    val originIpc = Native2JsIpc(portId, this@JsMicroModule).also {
                        beConnect(it, Request(Method.GET, "file://$mmid/event/dns/connect"))
                    }

                    /**
                     * 将两个消息通道间接互联
                     */
                    originIpc.onMessage { (ipcMessage) ->
                        targetIpc.postMessage(ipcMessage)
                    }
                    targetIpc.onMessage { (ipcMessage) ->
                        originIpc.postMessage(ipcMessage)
                    }
                }
            }
            null
        }

        debugJMM("running!!", mmid)
        _ipcSet.add(streamIpc);
    }

    // 关停js 流
    private val closeJsProcessSignal = SimpleSignal()

    override suspend fun _shutdown() {
        /// TODO 发送指令，关停js进程
        debugJMM("closeJsProcessSignal emit", "$mmid/$metadata")
        closeJsProcessSignal.emit()
        processId = null
    }


    init {
//        onConnect { (clientIpc, reason) ->
//            return@onConnect null
////            clientIpc.ro
//            if (clientIpc.remote.mmid == "js.sys.dweb") {
//                return@onConnect null
//            }
//
//            /**
//             * 我们需要建立一个到js环境里的ipc连接，来与外部通讯
//             */
//            val serverPortId = createConnectPortId(this, IPC_ROLE.SERVER)
//            /**
//             * 将这两个消息通道进行连接
//             */
//            /**
//             * 如果 发起者也是 MessagePortIpc，那么我们可以直接销毁它在native的绑定，让这两个ipc直接通讯
//             */
//            if (clientIpc is MessagePortIpc) {
//                val serverPort = ALL_MESSAGE_PORT_CACHE.remove(serverPortId)!!
//                val clientPort = clientIpc.port
//                /**
//                 * 两个消息通道间接互联
//                 */
//                clientPort.onWebMessage { message -> serverPort.postMessage(message) }
//                serverPort.onWebMessage { message -> clientPort.postMessage(message) }
//                /**
//                 * 只做简单的销毁，不做关闭，从而不在触发 native 侧的 onMessage，减少解码开销
//                 */
//                clientIpc.destroy(false)
//            } else {
//                val serverIpc = createConnectIpc(clientIpc.remote, this, serverPortId)
//                /**
//                 * 两个消息通道间接互联
//                 */
//                serverIpc.onMessage { (ipcMessage) -> clientIpc.postMessage(ipcMessage) }
//                clientIpc.onMessage { (ipcMessage) -> serverIpc.postMessage(ipcMessage) }
//            }
//            null
//        }
    }
}