package info.bagen.rust.plaoc.microService.sys.jmm

import info.bagen.rust.plaoc.datastore.JmmMetadataDB
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.*
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import info.bagen.rust.plaoc.microService.sys.jmm.ui.JmmManagerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.http4k.core.Method
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

class JmmNMM : NativeMicroModule("jmm.sys.dweb") {
    companion object {
        private val apps = mutableMapOf<Mmid, JsMicroModule>()

        fun getAndUpdateJmmNmmApps() = apps

        suspend fun NativeMicroModule.nativeFetchFromJS(mmid: Mmid) {
            nativeFetch(
                Uri.of("file://dns.sys.dweb/open")
                    .query("app_id", mmid.encodeURIComponent())
            )
        }

        suspend fun NativeMicroModule.nativeFetchInstallDNS(jmmMetadata: JmmMetadata) { // 安装完成后，需要注册到 DnsNMM 中
            nativeFetch(
                Uri.of("file://dns.sys.dweb/install")
                    .query("jmmMetadata", gson.toJson(jmmMetadata))
            )
        }

        suspend fun NativeMicroModule.nativeFetchInstallApp(jmmMetadata: JmmMetadata, url: String) {
            nativeFetch(
                Uri.of("file://jmm.sys.dweb/install")
                    .query("mmid", jmmMetadata.id).query("metadataUrl", url)
            )
        }
    }

    init {
        // TODO 启动的时候，从数据库中恢复 apps 对象
        GlobalScope.launch(ioAsyncExceptionHandler) {
            JmmMetadataDB.queryJmmMetadataList().collectLatest {
                apps.clear()
                it.forEach { (key, value) ->
                    apps[key] = JsMicroModule(value)
                    nativeFetch(
                        Uri.of("file://dns.sys.dweb/install")
                            .query("jmmMetadata", gson.toJson(value))
                    )
                }
            }
        }
    }

    val queryMetadataUrl = Query.string().required("metadataUrl")
    val queryMmid = Query.string().required("mmid")

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {

        apiRouting = routes(
            "/install" bind Method.GET to defineHandler { request ->
                val metadataUrl = queryMetadataUrl(request)
                val jmmMetadata =
                    nativeFetch(metadataUrl).json<JmmMetadata>(JmmMetadata::class.java)
                // 根据 jmmMetadata 打开一个应用信息的界面，用户阅读界面信息后，可以点击"安装"
                openJmmMetadataInstallPage(jmmMetadata) /*{ metadata ->
                    JsMicroModule(metadata).apply {
                        apps[jmmMetadata.id] = this // 添加应用
                        bootstrapContext.dns.install(this) // 注册应用
                    }
                }*/
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
        jmmMetadata: JmmMetadata/*, installDNS: (JmmMetadata) -> Unit*/
    ) {
        // 打开安装的界面
        JmmManagerActivity.startActivity(jmmMetadata)
        // 拿到安装的状态
        /*val observe = DownLoadObserver(jmmMetadata.id)
        GlobalScope.launch(Dispatchers.IO) {
            observe.observe {
                if (it.downLoadStatus == DownLoadStatus.INSTALLED) {
                    installDNS(jmmMetadata)
                }
            }
        }*/
    }

    private fun openJmmMatadataUninstallPage(jmmMetadata: JmmMetadata) {
        TODO("Not yet implemented")
    }

    override suspend fun _shutdown() {

    }

}
