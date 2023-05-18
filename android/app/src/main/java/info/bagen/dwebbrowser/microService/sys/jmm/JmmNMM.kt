package info.bagen.dwebbrowser.microService.sys.jmm

import androidx.compose.runtime.mutableStateMapOf
import info.bagen.dwebbrowser.datastore.JmmMetadataDB
import info.bagen.dwebbrowser.microService.core.BootstrapContext
import info.bagen.dwebbrowser.microService.core.NativeMicroModule
import info.bagen.dwebbrowser.microService.helper.Mmid
import info.bagen.dwebbrowser.microService.helper.encodeURIComponent
import info.bagen.dwebbrowser.microService.helper.ioAsyncExceptionHandler
import info.bagen.dwebbrowser.microService.helper.json
import info.bagen.dwebbrowser.microService.sys.dns.nativeFetch
import info.bagen.dwebbrowser.microService.sys.jmm.ui.JmmManagerActivity
import info.bagen.dwebbrowser.microService.sys.mwebview.dwebServiceWorker.ServiceWorkerEvent
import info.bagen.dwebbrowser.microService.sys.mwebview.dwebServiceWorker.emitEvent
import info.bagen.dwebbrowser.service.DownLoadController
import info.bagen.dwebbrowser.service.compareAppVersionHigh
import info.bagen.dwebbrowser.util.DwebBrowserUtil
import info.bagen.dwebbrowser.util.FilesUtil
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

/**
 * 获取 map 值，如果不存在，则使用defaultValue; 如果replace 为true也替换为defaultValue
 */
inline fun <K, V> MutableMap<K, V>.getOrPutOrReplace(
    key: K, replaceValue: (V) -> V, defaultValue: () -> V
): V {
    val value = get(key)
    return if (value == null) {
        val answer = defaultValue()
        put(key, answer)
        answer
    } else {
        replaceValue(value)
    }
}

@OptIn(DelicateCoroutinesApi::class)
class JmmNMM : NativeMicroModule("jmm.sys.dweb") {
    companion object {
        private val apps = mutableStateMapOf<Mmid, JsMicroModule>()
        fun getAndUpdateJmmNmmApps() = apps

        private val controllerList = mutableListOf<JmmController>()
        val jmmController get() = controllerList.firstOrNull()
    }

    init {
        controllerList.add(JmmController(this))
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
            JmmMetadataDB.queryJmmMetadataList().collectLatest { maps -> // TODO 只要datastore更新，这边就会实时更新
                debugJMM("init/JmmNMM", "init Apps list -> ${maps.size}")

                maps.forEach { (key, value) ->
                    apps.getOrPutOrReplace(key, replaceValue = { local ->
                        if (compareAppVersionHigh(local.metadata.version, value.version)) {
                            local.metadata = value
                            emitEvent(value.id, ServiceWorkerEvent.UpdateFound.event)
                        }
                        local
                    }) {
                        JsMicroModule(value).also { jsMicroModule ->
                            try {
                                bootstrapContext.dns.install(jsMicroModule)
                            } catch (_: Exception) {
                            }
                        }
                    }
                    /*apps.getOrPut(key) {
                        JsMicroModule(value).also { jsMicroModule ->
                            try {
                                bootstrapContext.dns.install(jsMicroModule)
                            } catch (_: Exception) {
                            }
                        }
                    }*/
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
                debugJMM("install",jmmMetadata)
                // 根据 jmmMetadata 打开一个应用信息的界面，用户阅读界面信息后，可以点击"安装"
                openJmmMetadataInstallPage(jmmMetadata)
                return@defineHandler jmmMetadata
            },
            "/uninstall" bind Method.GET to defineHandler { request, ipc ->
                val mmid = queryMmid(request)
                apps[mmid]?.let { jsMicroModule ->
                    openJmmMetadataUninstallPage(jsMicroModule)
                } ?: return@defineHandler false
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
            /**继续下载*/
            "/resume" bind Method.GET to defineHandler { _, ipc ->
                debugJMM("resume",ipc.remote.mmid)
                DwebBrowserUtil.INSTANCE.mBinderService?.invokeUpdateDownloadStatus(
                    ipc.remote.mmid, DownLoadController.RESUME
                )
            },
            "/cancel" bind Method.GET to defineHandler { _, ipc ->
                debugJMM("cancel",ipc.remote.mmid)
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
    private  fun openJmmMetadataInstallPage(jmmMetadata: JmmMetadata) {
        // 打开安装的界面
        JmmManagerActivity.startActivity(jmmMetadata)
    }

    private fun openJmmMetadataUninstallPage(jsMicroModule: JsMicroModule) {
        // 先从列表移除，然后删除文件
        apps.remove(jsMicroModule.metadata.id)
        bootstrapContext.dns.uninstall(jsMicroModule)
        FilesUtil.uninstallApp(jsMicroModule.metadata.id)
    }
    override suspend fun _shutdown() {

    }

}
