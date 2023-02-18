package info.bagen.rust.plaoc.microService

import android.webkit.MimeTypeMap
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.network.fetchAdaptor
import kotlinx.coroutines.runBlocking
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.io.File
import kotlin.io.path.Path


typealias Domain = String;

val global_dns = DwebDNS()

class DwebDNS() : NativeMicroModule("dns.sys.dweb") {
    private val mmMap = mutableMapOf<Domain, MicroModule>()

    private val jsProcessNMM = JsProcessNMM()

    private val bootNMM = BootNMM()
    private val multiWebViewNMM = MultiWebViewNMM()
    private val httpNMM = HttpNMM()

    override suspend fun _bootstrap() {
        install(this)
        running_apps[this.mmid] = this
        install(bootNMM)
        install(jsProcessNMM)
        install(multiWebViewNMM)
        install(httpNMM)

        /// 对全局的自定义路由提供适配器
        /** 对等连接列表 */
        val connects = mutableMapOf<MicroModule, MutableMap<Mmid, Ipc>>()
        fetchAdaptor = { fromMM, request ->
            if (request.uri.scheme == "file" && request.uri.host.endsWith(".dweb")) {
                val mmid = request.uri.host
                mmMap[mmid]?.let {

                    /** 一个互联实例表 */
                    val ipcMap = connects.getOrPut(fromMM) { mutableMapOf() }

                    /**
                     * 一个互联实例
                     */
                    val ipc = ipcMap.getOrPut(mmid) {
                        val toMM = open(mmid);
                        toMM.connect(fromMM).also { ipc ->
                            // 在 IPC 关闭的时候，从 ipcMap 中移除
                            ipc.onClose { ipcMap.remove(mmid); }
                        }
                    }
                    return@let ipc.request(request).asResponse()
                }
            } else if (request.uri.scheme == "file" && request.uri.host == "") {
                val prefixUrl = ""
                try {
                    val filePath = Path(prefixUrl, request.uri.path).toString()
                    val stats = File(filePath)
                    if (stats.isDirectory) {
                        throw Exception(stats.toString())
                    }
                    val ext = stats.extension
                    val mime: MimeTypeMap = MimeTypeMap.getSingleton()
                    val type: String = mime.getExtensionFromMimeType(ext) ?: "application/octet-stream"
                    Response(status = Status.OK)
                        .headers(
                            headers = listOf(
                                Pair("Content-Length", stats.length().toString()),
                                Pair("Content-Type", type)
                            )
                        )
                }catch (e: Throwable) {
                    Response(Status.NOT_FOUND).body(e.message?:"the ${request.uri.path} file not found ")
                }
            } else null
        }
        val query_app_id = Query.string().required("app_id")

        /// 定义路由功能
        apiRouting = routes(
            "/open" bind Method.GET to defineHandler { request ->
                open(query_app_id(request))
                true
            },
            "/close" bind Method.GET to defineHandler { request ->
                close(query_app_id(request))
                true
            }
        )
        /// 启动 boot 模块
        runBlocking {
            open("boot.sys.dweb")
        }
    }

    override suspend fun _shutdown() {
        fetchAdaptor = null
        mmMap.forEach {
            it.value.shutdown()
        }
        mmMap.clear()
    }

    private val running_apps = mutableMapOf<Mmid, MicroModule>();

    /** 安装应用 */
    private fun install(mm: MicroModule) {
        mmMap[mm.mmid] = mm
    }

    /** 查询应用 */
    private suspend fun query(mmid: Mmid): MicroModule? {
        return mmMap[mmid]
    }

    /** 打开应用 */
    private suspend fun open(mmid: Mmid): MicroModule {
        return running_apps.getOrPut(mmid) {
            println("MicroModule#running_apps===>${mmid}  ")
            query(mmid)?.also {
                it.bootstrap()
            } ?: throw  Exception("no found app: $mmid")
        }
    }

    /** 关闭应用 */
    private suspend fun close(mmid: Mmid): Int {
        return running_apps.remove(mmid)?.let {
            try {
                it.shutdown()
                1
            } catch (_: Throwable) {
                0
            }
        } ?: -1
    }
}



