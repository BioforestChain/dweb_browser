package info.bagen.rust.plaoc.microService.sys.jmm

import android.util.Log
import info.bagen.rust.plaoc.datastore.JmmMetadataDB
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.*
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import info.bagen.rust.plaoc.microService.sys.jmm.ui.DownLoadStatus
import info.bagen.rust.plaoc.microService.sys.jmm.ui.JmmManagerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

class JmmNMM : NativeMicroModule("jmm.sys.dweb") {
    companion object {
        const val hostName = "file://jmm.sys.dweb"
        private val apps = mutableMapOf<Mmid, JsMicroModule>()

        fun getAndUpdateJmmNmmApps() = apps

        fun nativeFetch(mmid: Mmid) {
            GlobalScope.launch(Dispatchers.IO) {
                apps[mmid]?.nativeFetch(
                    "file://dns.sys.dweb/open?app_id=${mmid.encodeURIComponent()}"
                ) ?: Log.e("JmmNMM", "no found jmm mmid $mmid")
            }
        }

        fun nativeFetchJMM(jmmMetadata: JmmMetadata, url: String) {
            GlobalScope.launch(Dispatchers.IO) {
                apps.getOrElse(jmmMetadata.id) {
                    JsMicroModule(jmmMetadata)
                }.nativeFetch("${hostName}/install?mmid=${jmmMetadata.id}&metadataUrl=$url")
            }
        }
    }

    init {
        // TODO 启动的时候，从数据库中恢复 apps 对象
        GlobalScope.launch {
            apps.clear()
            JmmMetadataDB.queryJsMicroModuleList().collect {
                it.forEach { (key, value) ->
                    apps[key] = value
                }
            }
        }
    }

    val queryMetadataUrl = Query.string().required("metadataUrl")
    val queryMmid = Query.string().required("mmid")

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        // 初始化的DNS注册
        for (app in apps.values) {
            bootstrapContext.dns.install(app)
        }

        apiRouting = routes(
            "/install" bind Method.GET to defineHandler { request ->
                val metadataUrl = queryMetadataUrl(request)
                val jmmMetadata =
                    nativeFetch(metadataUrl).json<JmmMetadata>(JmmMetadata::class.java)
                // 根据 jmmMetadata 打开一个应用信息的界面，用户阅读界面信息后，可以点击"安装"
                openJmmMetadataInstallPage(jmmMetadata) { metadata ->
                    JsMicroModule(metadata).apply {
                        JmmMetadataDB.saveJsMicroModule(jmmMetadata.id, this)
                        apps[jmmMetadata.id] = this // 添加应用
                        bootstrapContext.dns.install(this) // 注册应用
                    }
                }
                return@defineHandler jmmMetadata
            },
            "/uninstall" bind Method.GET to defineHandler { request, ipc ->
                val mmid = queryMmid(request)
                val jmm = apps[mmid] ?: throw Exception("")
                openJmmMatadataUninstallPage(jmm.metadata)
                return@defineHandler true
            },
            "/query" bind Method.GET to defineHandler { request, ipc ->
                return@defineHandler AppsQueryResult(
                    apps.map { it.value.metadata },
                    installingApps.map { it.value })
            },
            "/download" bind Method.GET to defineHandler { request ->
                val mmid = queryMmid(request)
                apps.getOrPut(mmid) {
                    JsMicroModule(JmmMetadata(
                        id = mmid,
                        server = JmmMetadata.MainServer(
                            root = "file:///bundle", entry = "/cot.worker.js"))
                    ).also {
                        bootstrapContext.dns.install(it) // 注册应用
                    }
                }
                return@defineHandler true
            }
        )

    }

    data class AppsQueryResult(
        val installedAppList: List<JmmMetadata>,
        val installingAppList: List<InstallingAppInfo>
    )

    data class InstallingAppInfo(var progress: Float, val jmmMetadata: JmmMetadata)

    private val installingApps = mutableMapOf<Mmid, InstallingAppInfo>()
    private suspend fun openJmmMetadataInstallPage(
        jmmMetadata: JmmMetadata, installDNS: (JmmMetadata) -> Unit
    ) {
        // 打开安装的界面
        JmmManagerActivity.startActivity(jmmMetadata)
        // 拿到安装的状态
        val observe = DownLoadObserver(jmmMetadata.id)
        GlobalScope.launch(Dispatchers.IO) {
            observe.observe {
                if (it.downLoadStatus == DownLoadStatus.INSTALLED) {
                    installDNS(jmmMetadata)
                }
            }
        }
        /*when(DwebBrowserService.poDownLoadStatus[jmmMetadata.id]?.waitPromise()) {
            DownLoadStatus.INSTALLED -> installDNS(jmmMetadata)
            else -> { }
        }*/
    }

    private fun openJmmMatadataUninstallPage(jmmMetadata: JmmMetadata) {
        TODO("Not yet implemented")
    }

    override suspend fun _shutdown() {

    }

}
