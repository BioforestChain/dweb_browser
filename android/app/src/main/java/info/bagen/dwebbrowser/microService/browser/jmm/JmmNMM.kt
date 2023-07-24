package info.bagen.dwebbrowser.microService.browser.jmm

import androidx.compose.runtime.mutableStateMapOf
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.debugBrowser
import org.dweb_browser.microservice.help.Mmid
import org.dweb_browser.helper.*
import org.dweb_browser.microservice.sys.dns.nativeFetch
import info.bagen.dwebbrowser.microService.browser.jmm.ui.JmmManagerActivity
import org.dweb_browser.microservice.help.DWEB_DEEPLINK
import org.dweb_browser.browserUI.download.DownLoadController
import org.dweb_browser.browserUI.download.compareAppVersionHigh
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.dweb_browser.browserUI.database.AppInfoDataStore
import org.dweb_browser.browserUI.util.BrowserUIApp
import org.dweb_browser.browserUI.util.FilesUtil
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.json
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.net.URL

fun debugJMM(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("JMM", tag, msg, err)

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
class JmmNMM : NativeMicroModule("jmm.browser.dweb") {

  enum class EIpcEvent(val event:String){
    State("state"),
    Ready("ready"),
    Activity("activity"),
    Close("close")
  }

  override val dweb_deeplinks = mutableListOf<DWEB_DEEPLINK>("dweb:install")

  companion object {
    private val apps = mutableStateMapOf<Mmid, JsMicroModule>()
    fun getAndUpdateJmmNmmApps() = apps

    private val controllerList = mutableListOf<JmmController>()
    val jmmController get() = controllerList.firstOrNull()
  }

  init {
    controllerList.add(JmmController(this))
  }

  /** 启动的时候，从数据库中恢复 apps 对象*/
  private fun recoverAppData() {
    debugJMM("init/JmmNMM", "recoverAppData")
    GlobalScope.launch(ioAsyncExceptionHandler) {
      AppInfoDataStore.queryAppInfoList().collectLatest { maps -> // TODO 只要datastore更新，这边就会实时更新
        debugJMM("init/JmmNMM", "init Apps list -> ${maps.size}")

        maps.forEach { appMetadata ->
          apps.getOrPutOrReplace(appMetadata.id, replaceValue = { local ->
            debugJMM(
              "update getOrPutOrReplace",
              "old version:${local.metadata.version} new:${appMetadata.version} ${
                compareAppVersionHigh(
                  local.metadata.version,
                  appMetadata.version
                )
              }"
            )
            if (compareAppVersionHigh(local.metadata.version, appMetadata.version)) {
              jmmController?.closeApp(local.mmid)
            }
            JsMicroModule(appMetadata).also { jsMicroModule ->
              bootstrapContext.dns.install(jsMicroModule)
            }
          }) {
            apps.getOrPut(appMetadata.id) {
              JsMicroModule(appMetadata).also { jsMicroModule ->
                bootstrapContext.dns.install(jsMicroModule)
              }
            }
          }
        }
      }
    }
  }

  val queryMetadataUrl = Query.string().required("url")
  val queryMmid = Query.string().required("app_id")

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    recoverAppData()
    apiRouting = routes(
      "/install" bind Method.GET to defineHandler { request ->
        val metadataUrl = queryMetadataUrl(request)
        val appMetaData =
          nativeFetch(metadataUrl).json<AppMetaData>(AppMetaData::class.java)
        val url = URL(metadataUrl)
        // 根据 jmmMetadata 打开一个应用信息的界面，用户阅读界面信息后，可以点击"安装"
        jmmMetadataInstall(appMetaData, url)
        return@defineHandler appMetaData
      },
      "/uninstall" bind Method.GET to defineHandler { request ->
        val mmid = queryMmid(request)
        debugJMM("uninstall", mmid)
        jmmMetadataUninstall(mmid)
        return@defineHandler true
      },
      "/closeApp" bind Method.GET to defineHandler { request ->
        val mmid = queryMmid(request)
        jmmController?.closeApp(mmid)
        return@defineHandler true
      },
      // app详情
      "/detailApp" bind Method.GET to defineHandler{ request ->
        val mmid = queryMmid(request)
        debugBrowser("detailApp",mmid)
        val apps = getAndUpdateJmmNmmApps()
        val metadata = apps[mmid]?.metadata
          ?: return@defineHandler Response(Status.NOT_FOUND).body("not found ${mmid}")
        JmmManagerActivity.startActivity(metadata)
        return@defineHandler  true
      },
      "/pause" bind Method.GET to defineHandler { _, ipc ->
        BrowserUIApp.Instance.mBinderService?.invokeUpdateDownloadStatus(
          ipc.remote.mmid, DownLoadController.PAUSE
        )
      },
      /**继续下载*/
      "/resume" bind Method.GET to defineHandler { _, ipc ->
        debugJMM("resume", ipc.remote.mmid)
        BrowserUIApp.Instance.mBinderService?.invokeUpdateDownloadStatus(
          ipc.remote.mmid, DownLoadController.RESUME
        )
      },
      "/cancel" bind Method.GET to defineHandler { _, ipc ->
        debugJMM("cancel", ipc.remote.mmid)
        BrowserUIApp.Instance.mBinderService?.invokeUpdateDownloadStatus(
          ipc.remote.mmid, DownLoadController.CANCEL
        )
      }
    )

  }

  private fun jmmMetadataInstall(appMetaData: AppMetaData, url: URL) {
    if (!appMetaData.bundle_url.startsWith("http")) {
      appMetaData.bundle_url = URL(url, appMetaData.bundle_url).toString()
    }
    debugJMM("openJmmMetadataInstallPage", appMetaData.bundle_url)
    // 打开安装的界面
    JmmManagerActivity.startActivity(appMetaData)
  }

  private suspend fun jmmMetadataUninstall(mmid: Mmid) {
    // 先从列表移除，然后删除文件
    apps.remove(mmid)
    bootstrapContext.dns.uninstall(mmid)
    AppInfoDataStore.deleteAppInfo(mmid)
    FilesUtil.uninstallApp(App.appContext, mmid)
  }

  override suspend fun _shutdown() {

  }

}
