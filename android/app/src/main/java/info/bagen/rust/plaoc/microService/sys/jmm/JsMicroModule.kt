package info.bagen.rust.plaoc.microService.sys.jmm

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.core.connectAdapterManager
import info.bagen.rust.plaoc.microService.helper.int
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.helper.stream
import info.bagen.rust.plaoc.microService.helper.toBase64Url
import info.bagen.rust.plaoc.microService.ipc.IPC_ROLE
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.IpcResponse
import info.bagen.rust.plaoc.microService.ipc.ReadableStreamIpc
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.ALL_MESSAGE_PORT_CACHE
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.MessagePortIpc
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.Native2JsIpc
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import org.http4k.core.*
import org.http4k.lens.Query
import org.http4k.lens.string
import java.util.*


inline fun debugJMM(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("jmm", tag, msg, err)


open class JsMicroModule(val metadata: JmmMetadata) : MicroModule() {
    companion object {


        suspend fun createConnectPortId(jsMM: JsMicroModule, role: IPC_ROLE): Int {
            val processId = jsMM.processId
                ?: throw Exception("$${jsMM.mmid} process_id no found, should bootstrap first")

            val portId = jsMM.nativeFetch(
                "file://js.sys.dweb/create-ipc?process_id=$processId&role=${role.role}"
            ).int();
            return portId
        }

        suspend fun createConnectIpc(
            otherMM: Ipc.MicroModuleInfo, jsMM: JsMicroModule, role: IPC_ROLE
        ): Ipc {
            val portId = createConnectPortId(jsMM, role)
            val otherIpc = Native2JsIpc(portId, jsMM);
            return otherIpc
        }

        suspend fun createConnectIpc(
            otherMM: Ipc.MicroModuleInfo, jsMM: JsMicroModule, portId: Int
        ): Ipc {
            val otherIpc = Native2JsIpc(portId, jsMM);
            return otherIpc
        }

        init {
            connectAdapterManager.append { fromMM, toMM, reason ->
                /*if (fromMM is JsMicroModule) {
                    */
                /**
                 * 主动连接，不需要触发 fromMM.beConnect
                 * 因为这一步是在js里头自己会去做
                 *//*
                    val toIpc = createConnectIpc(toMM, fromMM, IPC_ROLE.CLIENT)
                    toIpc
                } else */if (toMM is JsMicroModule) {
                val fromIpc = createConnectIpc(fromMM, toMM, IPC_ROLE.SERVER)
                fromMM.beConnect(fromIpc, reason)
                fromIpc
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

        val query_mmid = Query.string().required("mmid")
        val query_cid = Query.string().required("cid")

        val ipc_js_sys_dweb = bootstrapContext.dns.connect("js.sys.dweb")
        ipc_js_sys_dweb.onRequest { (request) ->
            if (request.uri.path == "/dns/connect") {
                val req = Request(Method.GET, request.uri)
                val mmid = query_mmid(req)
                val cid = query_cid(req)
                /**
                 * 作为 fromMM，发起连接
                 * 会触发 fromMM.beConnect
                 */
                bootstrapContext.dns.connect(mmid, req)
            }
        }

        debugJMM("running!!", mmid)

        _ipcSet.add(streamIpc);


    }


    override suspend fun _shutdown() {
        /// TODO 发送指令，关停js进程
        processId = null
    }


    init {
        onConnect { (clientIpc) ->
            return@onConnect null
//            clientIpc.ro
            if (clientIpc.remote.mmid == "js.sys.dweb") {
                return@onConnect null
            }

            /**
             * 我们需要建立一个到js环境里的ipc连接，来与外部通讯
             */
            val serverPortId = createConnectPortId(this, IPC_ROLE.SERVER)
            /**
             * 将这两个消息通道进行连接
             */
            /**
             * 如果 发起者也是 MessagePortIpc，那么我们可以直接销毁它在native的绑定，让这两个ipc直接通讯
             */
            if (clientIpc is MessagePortIpc) {
                val serverPort = ALL_MESSAGE_PORT_CACHE.remove(serverPortId)!!
                val clientPort = clientIpc.port
                /**
                 * 两个消息通道间接互联
                 */
                clientPort.onWebMessage { message -> serverPort.postMessage(message) }
                serverPort.onWebMessage { message -> clientPort.postMessage(message) }
                /**
                 * 只做简单的销毁，不做关闭，从而不在触发 native 侧的 onMessage，减少解码开销
                 */
                clientIpc.destroy(false)
            } else {
                val serverIpc = createConnectIpc(clientIpc.remote, this, serverPortId)
                /**
                 * 两个消息通道间接互联
                 */
                serverIpc.onMessage { (ipcMessage) -> clientIpc.postMessage(ipcMessage) }
                clientIpc.onMessage { (ipcMessage) -> serverIpc.postMessage(ipcMessage) }
            }
            null
        }
    }
}