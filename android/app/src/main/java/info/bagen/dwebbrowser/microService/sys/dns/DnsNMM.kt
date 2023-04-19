package info.bagen.dwebbrowser.microService.sys.dns

import info.bagen.dwebbrowser.microService.core.*
import info.bagen.dwebbrowser.microService.helper.*
import info.bagen.dwebbrowser.microService.ipc.Ipc
import info.bagen.dwebbrowser.microService.ipc.IpcEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.http4k.core.*
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

inline fun debugDNS(tag: String, msg: Any = "", err: Throwable? = null) =
    printdebugln("fetch", tag, msg, err)

class DnsNMM : NativeMicroModule("dns.sys.dweb") {
    private val installApps = mutableMapOf<Mmid, MicroModule>() // 已安装的应用
    private val runningApps = mutableMapOf<Mmid, MicroModule>() // 正在运行的应用

    suspend fun bootstrap() {
        if (!this.running) {
            bootstrapMicroModule(this)
        }
        onActivity()
    }

    /** 对等连接列表 */
    private val mmConnectsMap =
        mutableMapOf<MicroModule, MutableMap<Mmid, PromiseOut<ConnectResult>>>()
    private val mmConnectsMapLock = Mutex()

    /** 为两个mm建立 ipc 通讯 */
    private suspend fun connectTo(
      fromMM: MicroModule, toMmid: Mmid, reason: Request
    ) = mmConnectsMapLock.withLock {

        /** 一个互联实例表 */
        val connectsMap = mmConnectsMap.getOrPut(fromMM) { mutableMapOf() }

        /**
         * 一个互联实例
         */
        connectsMap.getOrPut(toMmid) {
            PromiseOut<ConnectResult>().also { po ->
                GlobalScope.launch(ioAsyncExceptionHandler) {
                    val toMM = open(toMmid);
                    debugFetch("DNS/connect", "${fromMM.mmid} => $toMmid")
                    val connects = connectMicroModules(fromMM, toMM, reason)
                    po.resolve(connects)
                    connects.ipcForFromMM.onClose {
                        mmConnectsMapLock.withLock {
                            connectsMap.remove(toMmid);
                        }
                    }
                }
            }
        }
    }.waitPromise()

    class MyDnsMicroModule(private val dnsMM: DnsNMM, private val fromMM: MicroModule) :
      DnsMicroModule {
        override fun install(mm: MicroModule) {
            // TODO 作用域保护
            dnsMM.install(mm)
        }

        override fun uninstall(mm: MicroModule) {
            // TODO 作用域保护
            dnsMM.uninstall(mm)
        }

        override suspend fun connect(
          mmid: Mmid, reason: Request?
        ): ConnectResult {
            // TODO 权限保护
            return dnsMM.connectTo(
                fromMM, mmid, reason ?: Request(Method.GET, Uri.of("file://$mmid"))
            )
        }

        override suspend fun bootstrap(mmid: Mmid) {
            dnsMM.open(mmid)
        }
    }

    class MyBootstrapContext(override val dns: MyDnsMicroModule) : BootstrapContext {}

    suspend fun bootstrapMicroModule(fromMM: MicroModule) {
        fromMM.bootstrap(MyBootstrapContext(MyDnsMicroModule(this@DnsNMM, fromMM)))
    }

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        install(this)
        runningApps[this.mmid] = this

        /**
         * 对全局的自定义路由提供适配器
         * 对 nativeFetch 定义 file://xxx.dweb的解析
         */
        _afterShutdownSignal.listen(nativeFetchAdaptersManager.append { fromMM, request ->
            if (request.uri.scheme == "file" && request.uri.host.endsWith(".dweb")) {
                val mmid = request.uri.host
                debugFetch(
                    "DNS/fetchAdapter",
                    "fromMM=${fromMM.mmid} >> requestMmid=$mmid: >> path=${request.uri.path} >> ${request.uri}"
                )
                installApps[mmid]?.let {
                    val (fromIpc) = connectTo(fromMM, mmid, request)
                    return@let fromIpc.request(request)
                } ?: Response(Status.BAD_GATEWAY).body(request.uri.toString())
            } else null
        })


        val query_app_id = Query.string().required("app_id")
        val query_jmm_metadata = Query.string().required("jmmMetadata")

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
                debugDNS("close/$mmid", request.uri.path)
                close(query_app_id(request))
                true
            },
            // 应用重启
            "/restart" bind Method.GET to defineHandler { request, ipc ->
                // 关闭后端连接
                close(ipc.remote.mmid)
                // 调用重启
                runBlockingCatching(ioAsyncExceptionHandler) {
                    // TODO 神奇的操作
                    delay(200)
                    bootstrapContext.dns.bootstrap(ipc.remote.mmid)
                }
            })
    }

    override suspend fun onActivity(event: IpcEvent, ipc: Ipc) {
        onActivity(event)
    }

    suspend fun onActivity(event: IpcEvent = IpcEvent.fromUtf8("activity", "")) {
        /// 启动 boot 模块
        open("boot.sys.dweb")
        connect("boot.sys.dweb").ipcForFromMM.postMessage(event)
    }

    override suspend fun _shutdown() {
        installApps.forEach {
            it.value.shutdown()
        }
        installApps.clear()
    }

    /** 安装应用 */
    fun install(mm: MicroModule) {
        installApps[mm.mmid] = mm
    }

    /** 卸载应用 */
    fun uninstall(mm: MicroModule) {
        installApps.remove(mm.mmid)
    }

    /** 查询应用 */
    private suspend inline fun query(mmid: Mmid): MicroModule? {
        return installApps[mmid]
    }

    /** 打开应用 */
    suspend fun open(mmid: Mmid): MicroModule {
        return runningApps.getOrPut(mmid) {
            query(mmid)?.also {
                bootstrapMicroModule(it)
            } ?: throw Exception("no found app: $mmid")
        }
    }

    /** 关闭应用 */
    suspend fun close(mmid: Mmid): Int {
        return runningApps.remove(mmid)?.let { microModule ->
            runCatching {
                mmConnectsMap[microModule]?.remove(mmid) // 将这个连接关闭
                microModule.shutdown()
                1
            }.getOrDefault(0)
        } ?: -1
    }
}



