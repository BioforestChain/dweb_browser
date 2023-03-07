package info.bagen.rust.plaoc.microService.sys.dns

import info.bagen.rust.plaoc.microService.core.*
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.ioAsyncExceptionHandler
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.ipc.Ipc
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.http4k.core.*
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

inline fun debugDNS(tag: String, msg: Any = "", err: Throwable? = null) =
    printdebugln("fetch", tag, msg, err)


class DnsNMM() : NativeMicroModule("dns.sys.dweb") {
    private val mmMap = mutableMapOf<Mmid, MicroModule>()

    suspend fun bootstrap() {
        bootstrapMicroModule(this)
    }

    /** 对等连接列表 */
    private val connects = mutableMapOf<MicroModule, MutableMap<Mmid, Ipc>>()

    /** 为两个mm建立 ipc 通讯 */
    private suspend fun connectTo(fromMM: MicroModule, toMmid: Mmid, reason: Request): Ipc {
        /** 一个互联实例表 */
        val ipcMap = connects.getOrPut(fromMM) { mutableMapOf() }

        /**
         * 一个互联实例
         */
        val ipc = ipcMap.getOrPut(toMmid) {
            val toMM = open(toMmid);
            debugFetch("DNS/connect", "${fromMM.mmid} => $toMmid")
            connectMicroModules(fromMM, toMM, reason).also { ipc ->
                // 在 IPC 关闭的时候，从 ipcMap 中移除
                ipc.onClose { ipcMap.remove(toMmid); }
            }
        }
        return ipc
    }

    class MyDnsMicroModule(private val dnsMM: DnsNMM, private val fromMM: MicroModule) :
        DnsMicroModule {
        override fun install(mm: MicroModule) {
            // TODO 作用域保护
            install(mm)
        }

        override fun uninstall(mm: MicroModule) {
            // TODO 作用域保护
            uninstall(mm)
        }

        override suspend fun connect(
            mmid: Mmid,
            reason: Request?
        ): Ipc {
            // TODO 权限保护
            return dnsMM.connectTo(
                fromMM,
                mmid,
                reason ?: Request(Method.GET, Uri.of("file://$mmid"))
            )
        }

    }

    class MyBootstrapContext(override val dns: MyDnsMicroModule) : BootstrapContext {}

    suspend fun bootstrapMicroModule(fromMM: MicroModule) {
        fromMM.bootstrap(MyBootstrapContext(MyDnsMicroModule(this, fromMM)))
    }

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        install(this)
        running_apps[this.mmid] = this

        /**
         * 对全局的自定义路由提供适配器
         * 对 nativeFetch 定义 file://xxx.dweb的解析
         */
        _afterShutdownSignal.listen(nativeFetchAdaptersManager.append { fromMM, request ->
            if (request.uri.scheme == "file" && request.uri.host.endsWith(".dweb")) {
                val mmid = request.uri.host
                debugFetch("DNS/fetchAdapter", "$mmid >> ${request.uri.path}")
                mmMap[mmid]?.let {
                    val ipc = connectTo(fromMM, mmid, request)
                    return@let ipc.request(request)
                } ?: Response(Status.BAD_GATEWAY).body(request.uri.toString())
            } else null
        })


        val query_app_id = Query.string().required("app_id")

        /// 定义路由功能
        apiRouting = routes(
            // 打开应用
            "/open" bind Method.GET to defineHandler { request ->
                debugDNS("open/$mmid", request.uri.path)
                open(query_app_id(request))
                true
            },
            // 关闭应用
            // TODO 能否关闭一个应该应该由应用自己决定
            "/close" bind Method.GET to defineHandler { request ->
                close(query_app_id(request))
                true
            })
        /// 启动 boot 模块
        GlobalScope.launch(ioAsyncExceptionHandler) {
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
    fun install(mm: MicroModule) {
        mmMap[mm.mmid] = mm
    }

    /** 卸载应用 */
    fun uninstall(mm: MicroModule) {
        mmMap.remove(mm.mmid)
    }


    /** 查询应用 */
    private suspend inline fun query(mmid: Mmid): MicroModule? {
        return mmMap[mmid]
    }

    /** 打开应用 */
    suspend fun open(mmid: Mmid): MicroModule {
        return running_apps.getOrPut(mmid) {
            query(mmid)?.also {
                bootstrapMicroModule(it)
            } ?: throw Exception("no found app: $mmid")
        }
    }

    /** 关闭应用 */
    suspend fun close(mmid: Mmid): Int {
        return running_apps.remove(mmid)?.let {
            runCatching {
                it.shutdown()
                1
            }.getOrDefault(0)
        } ?: -1
    }
}



