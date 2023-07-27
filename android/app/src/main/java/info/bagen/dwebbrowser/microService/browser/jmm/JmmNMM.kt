package info.bagen.dwebbrowser.microService.browser.jmm

import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.jmm.ui.JmmManagerActivity
import info.bagen.dwebbrowser.microService.core.AndroidNativeMicroModule
import org.dweb_browser.browserUI.database.AppInfoDataStore
import org.dweb_browser.browserUI.download.DownLoadController
import org.dweb_browser.browserUI.util.BrowserUIApp
import org.dweb_browser.browserUI.util.FilesUtil
import org.dweb_browser.microservice.help.DWEB_DEEPLINK
import org.dweb_browser.microservice.help.JmmAppInstallManifest
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.MMID
import org.dweb_browser.helper.printdebugln
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.help.json
import org.dweb_browser.microservice.sys.dns.nativeFetch
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

class JmmNMM : AndroidNativeMicroModule("jmm.browser.dweb","Js MicroModule Management") {

  override val short_name = "JMM";
  override val dweb_deeplinks = mutableListOf<DWEB_DEEPLINK>("dweb:install")
  override val categories = mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Hub_Service);

  enum class EIpcEvent(val event: String) {
    State("state"),
    Ready("ready"),
    Activity("activity"),
    Close("close")
  }


  companion object {
    private val controllerList = mutableListOf<JmmController>()
    val jmmController get() = controllerList.firstOrNull()

    fun installAppsContainMMid(mmid: MMID) =
      installAppList.find { it.jsMicroModule.mmid == mmid } != null

    fun installAppsMetadata(mmid: MMID) =
      installAppList.firstOrNull { it.jsMicroModule.mmid == mmid }?.jsMicroModule?.metadata
  }

  init {
    controllerList.add(JmmController(this))
  }

  val queryMetadataUrl = Query.string().required("url")
  val queryMmid = Query.string().required("app_id")

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    apiRouting = routes(
      "/install" bind Method.GET to defineHandler { request ->
        val metadataUrl = queryMetadataUrl(request)
        val jmmAppInstallManifest =
          nativeFetch(metadataUrl).json<JmmAppInstallManifest>(JmmAppInstallManifest::class.java)
        val url = URL(metadataUrl)
        // 根据 jmmMetadata 打开一个应用信息的界面，用户阅读界面信息后，可以点击"安装"
        jmmMetadataInstall(jmmAppInstallManifest, url)
        return@defineHandler jmmAppInstallManifest
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
      "/detailApp" bind Method.GET to defineHandler { request ->
        val mmid = queryMmid(request)
        debugJMM("detailApp", mmid)
        val apps = installAppList
        val metadata = apps.firstOrNull { it.jsMicroModule.mmid == mmid }?.jsMicroModule?.metadata
          ?: return@defineHandler Response(Status.NOT_FOUND).body("not found ${mmid}")
        JmmManagerActivity.startActivity(metadata)
        return@defineHandler true
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

  private fun jmmMetadataInstall(jmmAppInstallManifest: JmmAppInstallManifest, url: URL) {
    if (!jmmAppInstallManifest.bundle_url.startsWith("http")) {
      jmmAppInstallManifest.bundle_url = URL(url, jmmAppInstallManifest.bundle_url).toString()
    }
    debugJMM("openJmmMetadataInstallPage", jmmAppInstallManifest.bundle_url)
    // 打开安装的界面
    JmmManagerActivity.startActivity(jmmAppInstallManifest)
  }

  private suspend fun jmmMetadataUninstall(mmid: MMID) {
    // 先从列表移除，然后删除文件
    installAppList.removeIf { it.jsMicroModule.mmid == mmid }
    bootstrapContext.dns.uninstall(mmid)
    AppInfoDataStore.deleteAppInfo(mmid)
    FilesUtil.uninstallApp(App.appContext, mmid)
  }

  override suspend fun _shutdown() {

  }

}
