package info.bagen.rust.plaoc.microService.sys.jmm

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.helper.int
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.helper.rand
import info.bagen.rust.plaoc.microService.helper.stream
import info.bagen.rust.plaoc.microService.ipc.IPC_ROLE
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.IpcResponse
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.Native2JsIpc
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.ReadableStreamIpc
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import org.http4k.core.*


inline fun debugJMM(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("jmm", tag, msg, err)


open class JsMicroModule(val metadata: JmmMetadata) : MicroModule() {
    override val mmid get() = metadata.id

    /**
     * 和 dweb 的 port 一样，pid 是我们自己定义的，它跟我们的 mmid 关联在一起
     * 所以不会和其它程序所使用的 pid 冲突
     */
    private var processId: Int? = null
    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        debugJMM("bootstrap...", "$mmid/$metadata")
        val pid = rand(1, 1000)
        processId = pid
        val streamIpc = ReadableStreamIpc(this, IPC_ROLE.CLIENT)
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
                        .query("entry", metadata.server.entry)
                        .query("process_id", pid.toString())
                ).body(streamIpc.stream)
            ).stream(),
            "code-server"
        )

        debugJMM("running!!", mmid)

        _connectingIpcSet.add(streamIpc);
    }


    override suspend fun _connect(from: MicroModule): Ipc {
        val pid = processId ?: throw Exception("$mmid process_id no found, should bootstrap first")

        val portId = nativeFetch(
            "file://js.sys.dweb/create-ipc?process_id=$pid"
        ).int();
        val outerIpc = Native2JsIpc(portId, this);
        _connectingIpcSet.add(outerIpc)
        return outerIpc
    }

    private val _connectingIpcSet = mutableSetOf<Ipc>()


    override suspend fun _shutdown() {
        for (outerIpc in _connectingIpcSet) {
            outerIpc.close()
        }
        _connectingIpcSet.clear()

        /// TODO 发送指令，关停js进程
        processId = null
    }
}