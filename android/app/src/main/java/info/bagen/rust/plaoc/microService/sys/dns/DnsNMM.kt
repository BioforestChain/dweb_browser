package info.bagen.rust.plaoc.microService.sys.dns

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.DnsMicroModule
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.ipc.Ipc
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

inline fun debugDNS(tag: String, msg: Any = "", err: Throwable? = null) =
    printdebugln("fetch", tag, msg, err)


class DnsNMM() : NativeMicroModule("dns.sys.dweb"), DnsMicroModule {
    private val mmMap = mutableMapOf<Mmid, MicroModule>()
    private val bootstrapContext by lazy { DnsNMMBootstrapContext(this) }

    private class DnsNMMBootstrapContext(override val dns: DnsMicroModule) : BootstrapContext

    suspend fun bootstrap() {
        super.bootstrap(bootstrapContext)
    }

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        install(this)
        running_apps[this.mmid] = this

        /// 对全局的自定义路由提供适配器
        /** 对等连接列表 */
        val connects = mutableMapOf<MicroModule, MutableMap<Mmid, Ipc>>()
        /**
         * 对 nativeFetch 定义 file://xxx.dweb的解析
         */
        _afterShutdownSignal.listen(nativeFetchAdaptersManager.append { fromMM, request ->
            if (request.uri.scheme == "file" && request.uri.host.endsWith(".dweb")) {
                val mmid = request.uri.host
                debugFetch("DNS/fetchAdapter", "$mmid >> ${request.uri.path}")
                mmMap[mmid]?.let {
                    /** 一个互联实例表 */
                    val ipcMap = connects.getOrPut(fromMM) { mutableMapOf() }

                    /**
                     * 一个互联实例
                     */
                    val ipc = ipcMap.getOrPut(mmid) {
                        val toMM = open(mmid);
                        debugFetch("DNS/connect", "${toMM.mmid} 🥑 ${fromMM.mmid}")
                        toMM.connect(fromMM).also { ipc ->
                            // 在 IPC 关闭的时候，从 ipcMap 中移除
                            ipc.onClose { ipcMap.remove(mmid); }
                        }
                    }
                    return@let ipc.request(request)
                } ?: Response(Status.BAD_GATEWAY).body(request.uri.toString())
            } else null
        })
        val query_app_id = Query.string().required("app_id")

        /// 定义路由功能
        apiRouting = routes(
            "/open" bind Method.GET to defineHandler { request ->
                debugDNS("open/$mmid", request.uri.path)
                open(query_app_id(request))
                true
            },
            /**
             * TODO 能否关闭一个应该应该由应用自己决定
             */
            "/close" bind Method.GET to defineHandler { request ->
                close(query_app_id(request))
                true
            },
            "/install" bind Method.GET to defineHandler { request ->
//                install(mm)
                true
            }
        )
        /// 启动 boot 模块
        GlobalScope.launch {
            open("boot.sys.dweb")
        }
    }

    override suspend fun _shutdown() {
        mmMap.forEach {
            it.value.shutdown()
        }
        mmMap.clear()
    }

    private val running_apps = mutableMapOf<Mmid, MicroModule>();

    /** 安装应用 */
    override fun install(mm: MicroModule) {
        mmMap[mm.mmid] = mm
    }

    /** 卸载应用 */
    override fun uninstall(mm: MicroModule) {
        mmMap.remove(mm.mmid)
    }

    /** 查询应用 */
    private suspend inline fun query(mmid: Mmid): MicroModule? {
        return mmMap[mmid]
    }

    /** 打开应用 */
    private suspend fun open(mmid: Mmid): MicroModule {
        return running_apps.getOrPut(mmid) {
            query(mmid)?.also {
                it.bootstrap(bootstrapContext)
            } ?: throw Exception("no found app: $mmid")
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



