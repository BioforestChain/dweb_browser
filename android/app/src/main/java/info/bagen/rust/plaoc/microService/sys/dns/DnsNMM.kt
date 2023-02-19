package info.bagen.rust.plaoc.microService.sys.dns

import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.sys.http.net.fetchAdaptor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes


class DnsNMM() : NativeMicroModule("dns.sys.dweb") {
    private val mmMap = mutableMapOf<Mmid, MicroModule>()

    override suspend fun _bootstrap() {
        install(this)
        running_apps[this.mmid] = this

        /// 对全局的自定义路由提供适配器
        /** 对等连接列表 */
        val connects = mutableMapOf<MicroModule, MutableMap<Mmid, Ipc>>()
        fetchAdaptor = { fromMM, request ->
            if (request.uri.scheme == "file" && request.uri.host.endsWith(".dweb")) {
                val mmid = request.uri.host
                println("DNS#fetchAdaptor===>$mmid  ${request.uri.path}")
                mmMap[mmid]?.let {

                    /** 一个互联实例表 */
                    val ipcMap = connects.getOrPut(fromMM) { mutableMapOf() }

                    /**
                     * 一个互联实例
                     */
                    val ipc = ipcMap.getOrPut(mmid) {
                        val toMM = open(mmid);
                        println("DNS#toMM===>$mmid  $toMM  $fromMM")
                        toMM.connect(fromMM).also { ipc ->
                            // 在 IPC 关闭的时候，从 ipcMap 中移除
                            ipc.onClose { ipcMap.remove(mmid); }
                        }
                    }
                    return@let ipc.request(request)
                } ?: Response(Status.BAD_GATEWAY).body(request.uri.toString())
            } else null
        }
        val query_app_id = Query.string().required("app_id")

        /// 定义路由功能
        apiRouting = routes(
            "/open" bind Method.GET to defineHandler { request ->
                println("DNS#apiRouting===>$mmid  ${request.uri.path}")
                open(query_app_id(request))
                true
            },
            /**
             * TODO 能否关闭一个应该应该由应用自己决定
             */
            "/close" bind Method.GET to defineHandler { request ->
                close(query_app_id(request))
                true
            }
        )
        /// 启动 boot 模块
        GlobalScope.launch {
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
    fun install(mm: MicroModule) {
        mmMap[mm.mmid] = mm
    }

    /** 查询应用 */
    private suspend inline fun query(mmid: Mmid): MicroModule? {
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
            runCatching {
                it.shutdown()
                1
            }.getOrDefault(0)
        } ?: -1
    }
}



