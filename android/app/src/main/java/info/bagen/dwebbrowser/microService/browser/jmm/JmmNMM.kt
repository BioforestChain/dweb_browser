package info.bagen.dwebbrowser.microService.browser.jmm

import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.core.AndroidNativeMicroModule
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import org.dweb_browser.browserUI.database.JsMicroModuleStore
import org.dweb_browser.browserUI.download.DownLoadController
import org.dweb_browser.browserUI.download.compareAppVersionHigh
import org.dweb_browser.browserUI.util.BrowserUIApp
import org.dweb_browser.browserUI.util.FilesUtil
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.help.DWEB_DEEPLINK
import org.dweb_browser.microservice.help.JmmAppInstallManifest
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.MMID
import org.dweb_browser.microservice.help.MicroModuleManifest
import org.dweb_browser.microservice.help.json
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.sys.dns.nativeFetch
import org.dweb_browser.window.core.WindowState
import org.dweb_browser.window.core.constant.WindowMode
import org.dweb_browser.window.core.createWindowAdapterManager
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.net.URL

fun debugJMM(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("JMM", tag, msg, err)

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

class JmmNMM : AndroidNativeMicroModule("jmm.browser.dweb", "Js MicroModule Management") {

  override val short_name = "JMM";
  override val dweb_deeplinks = mutableListOf<DWEB_DEEPLINK>("dweb:install")
  override val categories =
    mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Hub_Service);

  enum class EIpcEvent(val event: String) {
    State("state"), Ready("ready"), Activity("activity"), Close("close")
  }

  private var jmmController: JmmController? = null
  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler

  fun getApps(mmid: MMID): MicroModuleManifest? {
    return bootstrapContext.dns.query(mmid)
  }

  val queryMetadataUrl = Query.string().required("url")
  val queryMmid = Query.string().required("app_id")

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    installJmmApps()

    this.onAfterShutdown {
      jmmController = null
    }

    val routeInstallHandler = defineHandler { request, ipc ->
      val metadataUrl = queryMetadataUrl(request)
      val jmmAppInstallManifest =
        nativeFetch(metadataUrl).json<JmmAppInstallManifest>()
      val url = URL(metadataUrl)
      // 根据 jmmMetadata 打开一个应用信息的界面，用户阅读界面信息后，可以点击"安装"
      jmmMetadataInstall(jmmAppInstallManifest, ipc, url)
      return@defineHandler jmmAppInstallManifest
    }
    apiRouting = routes(
      // 安装
      "install" bind Method.GET to routeInstallHandler,
      "/install" bind Method.GET to routeInstallHandler,
      "/uninstall" bind Method.GET to defineHandler { request ->
        val mmid = queryMmid(request)
        debugJMM("uninstall", mmid)
        jmmMetadataUninstall(mmid)
        return@defineHandler Response(Status.OK).body("""{"ok":true}""")
      },
      "/closeApp" bind Method.GET to defineHandler { request ->
        val mmid = queryMmid(request)
        jmmController?.closeApp(mmid)
        return@defineHandler Response(Status.OK).body("""{"ok":true}""")
      },
      // app详情
      "/detailApp" bind Method.GET to defineHandler { request, ipc ->
        val mmid = queryMmid(request)
        debugJMM("detailApp", mmid)
        val microModule = bootstrapContext.dns.query(mmid)
          ?: return@defineHandler Response(Status.NOT_FOUND).body("not found $mmid")

        // TODO: 系统原生应用如WebBrowser的详情页展示？
        if (microModule is JsMicroModule) {
          jmmMetadataInstall(microModule.metadata, ipc)
          return@defineHandler Response(Status.OK).body("ok")
        }

        return@defineHandler Response(Status.NOT_FOUND).body("not found $mmid")
      },
      "/pause" bind Method.GET to defineHandler { _, ipc ->
        BrowserUIApp.Instance.mBinderService?.invokeUpdateDownloadStatus(
          ipc.remote.mmid, DownLoadController.PAUSE
        )
        return@defineHandler Response(Status.OK).body("ok")
      },
      /**继续下载*/
      "/resume" bind Method.GET to defineHandler { _, ipc ->
        debugJMM("resume", ipc.remote.mmid)
        BrowserUIApp.Instance.mBinderService?.invokeUpdateDownloadStatus(
          ipc.remote.mmid, DownLoadController.RESUME
        )
        return@defineHandler Response(Status.OK).body("ok")
      },
      "/cancel" bind Method.GET to defineHandler { _, ipc ->
        debugJMM("cancel", ipc.remote.mmid)
        BrowserUIApp.Instance.mBinderService?.invokeUpdateDownloadStatus(
          ipc.remote.mmid, DownLoadController.CANCEL
        )
        return@defineHandler Response(Status.OK).body("ok")
      })
  }

  /**
   * 从内存中加载数据
   */
  private fun installJmmApps() {
    ioAsyncScope.launch {
      var preList = mutableListOf<JmmAppInstallManifest>()
      JsMicroModuleStore.queryAppInfoList().collectLatest { list -> // TODO 只要datastore更新，这边就会实时更新
        debugJMM("AppInfoDataStore", "size=${list.size}")
        /// 将会被卸载的应用
        val uninstalls = mutableMapOf<MMID, JmmAppInstallManifest>().also {
          for (jmmApp in preList) {
            it[jmmApp.id] = jmmApp
          }
        }
        list.map { jsMetaData ->
          // 如果存在，那么就不会卸载
          uninstalls.remove(jsMetaData.id);
          // 检测版本
          val lastAppMetaData = bootstrapContext.dns.query(jsMetaData.id)
          lastAppMetaData?.let {
            if (compareAppVersionHigh(it.version, jsMetaData.version)) {
              bootstrapContext.dns.close(it.mmid)
            }
          }
          bootstrapContext.dns.install(JsMicroModule(jsMetaData))
        }
        /// 将剩余的应用卸载掉
        for (jmmAppId in uninstalls.keys) {
          bootstrapContext.dns.uninstall(jmmAppId)
        }
        preList = list
      }
    }
  }

  private suspend fun jmmMetadataInstall(
    jmmAppInstallManifest: JmmAppInstallManifest, ipc: Ipc, url: URL? = null,
  ) {
    jmmController?.closeSelf() // 如果存在的话，关闭先，同时可以考虑置空
    if (!jmmAppInstallManifest.bundle_url.startsWith("http")) {
      url?.let {
        jmmAppInstallManifest.bundle_url = URL(it, jmmAppInstallManifest.bundle_url).toString()
      }
    }
    debugJMM("openJmmMetadataInstallPage", jmmAppInstallManifest.bundle_url)
    // 打开安装的界面
    // JmmManagerActivity.startActivity(jmmAppInstallManifest)
    // 打开安装窗口
    val win = createWindowAdapterManager.createWindow(
      WindowState(owner = mmid, provider = mmid, microModule = this).also {
        it.mode = WindowMode.MAXIMIZE
      }
    )
    withContext(mainAsyncExceptionHandler) { // 由于创建后，界面会同步执行，可能会存在主界面刷新界面比协程执行快，引发数据未初始化问题
      jmmController = JmmController(win, this@JmmNMM, jmmAppInstallManifest)
    }
  }

  private suspend fun jmmMetadataUninstall(mmid: MMID) {
    // 先从列表移除，然后删除文件
    bootstrapContext.dns.uninstall(mmid)
    JsMicroModuleStore.deleteAppInfo(mmid)
    FilesUtil.uninstallApp(App.appContext, mmid)
  }

  override suspend fun _shutdown() {
    ioAsyncScope.cancel()
  }
}
