package info.bagen.rust.plaoc.microService.sys.jmm

import android.util.Log
import info.bagen.rust.plaoc.datastore.JmmMetadataDB
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.*
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import info.bagen.rust.plaoc.microService.sys.jmm.ui.DownLoadStatus
import info.bagen.rust.plaoc.microService.sys.jmm.ui.JmmManagerActivity
import info.bagen.rust.plaoc.service.DwebBrowserService
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

class JmmNMM : NativeMicroModule("jmm.sys.dweb") {
    companion object {
        private val apps = mutableMapOf<Mmid, JsMicroModule>()

        fun getAndUpdateJmmNmmApps() = apps

        fun nativeFetch(mmid: Mmid) {
            runBlockingCatching {
                apps[mmid]?.nativeFetch(
                    "file://dns.sys.dweb/open?app_id=${mmid.encodeURIComponent()}"
                ) ?: Log.e("JmmNMM", "no found jmm mmid $mmid")
            }
        }

        fun nativeFetchJMM(jmmMetadata: JmmMetadata, url: String) {
            runBlockingCatching {
                apps.getOrElse(jmmMetadata.id) {
                    JsMicroModule(jmmMetadata)
                }.nativeFetch("file://jmm.sys.dweb/install?mmid=${jmmMetadata.id}&metadataUrl=$url")
            }
        }
    }

    init {
        // TODO 启动的时候，从数据库中恢复 apps 对象
        JmmMetadataDB.queryJmmMetadata()
    }

    val queryMetadataUrl = Query.string().required("metadataUrl")
    val queryMmid = Query.string().required("mmid")

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        // 初始化的DNS注册
        /*for (app in apps.values) {
            bootstrapContext.dns.install(app)
        }*/

        apiRouting = routes(
            "/install" bind Method.GET to defineHandler { request ->
                val metadataUrl = queryMetadataUrl(request)
                val jmmMetadata =
                    nativeFetch(metadataUrl).json<JmmMetadata>(JmmMetadata::class.java)
                // 根据 jmmMetadata 打开一个应用信息的界面，用户阅读界面信息后，可以点击"安装"
                openJmmMetadataInstallPage(jmmMetadata) { metadata ->
                    JsMicroModule(metadata).apply {
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
        when(DwebBrowserService.poDownLoadStatus[jmmMetadata.id]?.waitPromise()) {
            DownLoadStatus.INSTALLED -> installDNS(jmmMetadata)
            else -> { }
        }
        /*DownLoadStatusSubject.attach(jmmMetadata.id) { _: Mmid, downLoadStatus: DownLoadStatus ->
            when (downLoadStatus) {
                DownLoadStatus.INSTALLED -> { installDNS(jmmMetadata) }
                else -> { }
            }
        }*/
    }

    private fun openJmmMatadataUninstallPage(jmmMetadata: JmmMetadata) {
        TODO("Not yet implemented")
    }

    override suspend fun _shutdown() {

    }

}
