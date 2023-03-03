package info.bagen.rust.plaoc.microService.sys.jmm

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.helper.json
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import info.bagen.rust.plaoc.microService.sys.jmm.ui.DownLoadStatus
import info.bagen.rust.plaoc.microService.sys.jmm.ui.JmmManagerActivity
import org.http4k.core.Method
import org.http4k.format.Jackson.asJsonObject
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

class JmmNMM : NativeMicroModule("jmm.sys.dweb") {
    val apps = mutableMapOf<Mmid, JsMicroModule>()

    init {
        // TODO 启动的时候，从数据库中恢复 apps 对象
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
                openJmmMatadataInstallPage(jmmMetadata)
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
        )

    }

    data class AppsQueryResult(
        val installedAppList: List<JmmMetadata>,
        val installingAppList: List<InstallingAppInfo>
    ) {

    }

    data class InstallingAppInfo(var progress: Float, val jmmMetadata: JmmMetadata)

    private val installingApps = mutableMapOf<Mmid, InstallingAppInfo>()
    private suspend fun openJmmMatadataInstallPage(jmmMetadata: JmmMetadata) {
        // 打开安装的界面
        JmmManagerActivity.startActivity(jmmMetadata)
        // 拿到安装的状态
        val status = JmmManagerActivity.downLoadStatus[jmmMetadata.id]?.waitPromise()
        when(status) {
            DownLoadStatus.INSTALLED -> {
                registerJmmForDns(jmmMetadata)
            }
            DownLoadStatus.FAIL-> {
                // TODO 安装退出，或者安装失败的处理，也可以不处理
            }
            else -> {}
        }
    }
    /**
     * 注册应用
     * */
    private fun registerJmmForDns(jmmMetadata: JmmMetadata) {
        val jmm = JsMicroModule(jmmMetadata)
        // 添加应用
        apps[jmmMetadata.id] = jmm
    }

    private fun openJmmMatadataUninstallPage(jmmMetadata: JmmMetadata) {
        TODO("Not yet implemented")
    }

    override suspend fun _shutdown() {

    }

}
