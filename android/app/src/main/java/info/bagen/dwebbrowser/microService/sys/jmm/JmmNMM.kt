package info.bagen.dwebbrowser.microService.sys.jmm

import info.bagen.dwebbrowser.datastore.JmmMetadataDB
import info.bagen.dwebbrowser.microService.core.BootstrapContext
import info.bagen.dwebbrowser.microService.core.NativeMicroModule
import info.bagen.dwebbrowser.microService.helper.Mmid
import info.bagen.dwebbrowser.microService.helper.encodeURIComponent
import info.bagen.dwebbrowser.microService.helper.ioAsyncExceptionHandler
import info.bagen.dwebbrowser.microService.helper.json
import info.bagen.dwebbrowser.microService.sys.dns.nativeFetch
import info.bagen.dwebbrowser.microService.sys.jmm.ui.JmmManagerActivity
import info.bagen.dwebbrowser.microService.sys.nativeui.dwebServiceWorker.ServiceWorkerEvent
import info.bagen.dwebbrowser.microService.sys.nativeui.dwebServiceWorker.emitEvent
import info.bagen.dwebbrowser.service.DownLoadController
import info.bagen.dwebbrowser.util.DwebBrowserUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.http4k.core.Method
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

@OptIn(DelicateCoroutinesApi::class)
class JmmNMM : NativeMicroModule("jmm.sys.dweb") {
    companion object {
        private val apps = mutableMapOf<Mmid, JsMicroModule>()
        fun getAndUpdateJmmNmmApps() = apps

        /**获取当前App的数据配置*/
        fun getBfsMetaData(mmid: Mmid): JmmMetadata? {
            return apps[mmid]?.metadata
        }
    }

    init {
        // 启动的时候，从数据库中恢复 apps 对象
        GlobalScope.launch(ioAsyncExceptionHandler) {
            while (true) { // TODO 为了将 jmm.sys.dweb 启动，否则 bootstrapContext 会报错
                delay(1000)
                try {
                    nativeFetch(Uri.of("file://dns.sys.dweb/open")
                        .query("app_id", "jmm.sys.dweb".encodeURIComponent()))
                    break
                } catch (_: Exception) {}
            }
            JmmMetadataDB.queryJmmMetadataList().collectLatest { // TODO 只要datastore更新，这边就会实时更新
                debugJMM("init/JmmNMM", "init Apps list -> ${it.size}")
                it.forEach { (key, value) ->
                    // debugJMM("init/JmmNMM", "init Apps item -> $key==>$value")
                    apps.getOrPut(key) {
                        JsMicroModule(value).also { jsMicroModule ->
                            try {
                              bootstrapContext.dns.install(jsMicroModule)
                            } catch (_ : Exception) { }
                        }
                    }
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
            },
            "/pause" bind Method.GET to defineHandler { _, ipc ->
                DwebBrowserUtil.INSTANCE.mBinderService?.invokeUpdateDownloadStatus(
                    ipc.remote.mmid, DownLoadController.PAUSE
                )
            },
            /**重下*/
            "/resume" bind Method.GET to defineHandler { _, ipc ->
                // 重下触发开始事件
                emitEvent(ipc.remote.mmid,ServiceWorkerEvent.Start.event)
                DwebBrowserUtil.INSTANCE.mBinderService?.invokeUpdateDownloadStatus(
                    ipc.remote.mmid, DownLoadController.RESUME
                )
            },
            "/cancel" bind Method.GET to defineHandler { _, ipc ->
                // 触发取消事件
                emitEvent(ipc.remote.mmid,ServiceWorkerEvent.Cancel.event)
                DwebBrowserUtil.INSTANCE.mBinderService?.invokeUpdateDownloadStatus(
                    ipc.remote.mmid, DownLoadController.CANCEL
                )
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
