package info.bagen.rust.plaoc.microService.sys.jmm

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.json
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import info.bagen.rust.plaoc.microService.sys.jmm.ui.JmmManagerActivity
import org.http4k.core.Method
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
        for (app in apps.values) {
            bootstrapContext.dns.install(app)
        }

        apiRouting = routes(
            "/install" bind Method.GET to defineHandler { request ->

                val metadataUrl = queryMetadataUrl(request)
                val jmmMetadata =
                    nativeFetch(metadataUrl).json<JmmMetadata>(JmmMetadata::class.java)
                //jmmMetadata.fromRequest = request

                // TODO 根据 jmmMetadata 打开一个应用信息的界面，用户阅读界面信息后，可以点击"安装"
                //openJmmMatadataInstallPage(jmmMetadata)
                openJmmMatadataInstallPage(defaultJmmMetadata)

                true
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
    private fun openJmmMatadataInstallPage(jmmMetadata: JmmMetadata) {
        // TODO 下载解压压缩包
        // TODO 使用 file://dns.sys.dweb/install 进行注册
        JmmManagerActivity.startActivity(jmmMetadata)
    }

    private fun openJmmMatadataUninstallPage(jmmMetadata: JmmMetadata) {
        TODO("Not yet implemented")
    }

    override suspend fun _shutdown() {

    }

}