package info.bagen.dwebbrowser.microService.browser.jmm

import info.bagen.dwebbrowser.App
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.dweb_browser.browserUI.database.AppType
import org.dweb_browser.browserUI.database.DeskAppInfo
import org.dweb_browser.browserUI.database.DeskAppInfoStore
import org.dweb_browser.browserUI.download.DownLoadController
import org.dweb_browser.browserUI.download.isGreaterThan
import org.dweb_browser.browserUI.microService.browser.link.WebLinkMicroModule
import org.dweb_browser.browserUI.util.BrowserUIApp
import org.dweb_browser.browserUI.util.FilesUtil
import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.core.AndroidNativeMicroModule
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.help.bodyJson
import org.dweb_browser.microservice.help.types.DWEB_DEEPLINK
import org.dweb_browser.microservice.help.types.IMicroModuleManifest
import org.dweb_browser.microservice.help.types.JmmAppInstallManifest
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.types.MMID
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.sys.dns.nativeFetch
import org.dweb_browser.window.core.WindowState
import org.dweb_browser.window.core.constant.WindowConstants
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

fun debugJMM(tag: String, msg: Any? = "", err: Throwable? = null) = printDebug("JMM", tag, msg, err)

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
  init {
    short_name = "JMM";
    dweb_deeplinks = mutableListOf<DWEB_DEEPLINK>("dweb:install")
    categories =
      mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Hub_Service);
  }

  enum class EIpcEvent(val event: String) {
    State("state"), Ready("ready"), Activity("activity"), Close("close")
  }

  private var jmmController = JmmController(this@JmmNMM, getAppContext())

  fun getApps(mmid: MMID): IMicroModuleManifest? {
    return bootstrapContext.dns.query(mmid)
  }

  val queryMetadataUrl = Query.string().required("url")
  val queryMmid = Query.string().required("app_id")

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    installJmmApps()

    val routeInstallHandler = defineResponse {
      val metadataUrl = queryMetadataUrl(request)
      val response = nativeFetch(metadataUrl)
      if (response.status == Status.OK) {
        val jmmAppInstallManifest = response.bodyJson<JmmAppInstallManifest>()
        val url = URL(metadataUrl)
        // 根据 jmmMetadata 打开一个应用信息的界面，用户阅读界面信息后，可以点击"安装"
        installJsMicroModule(jmmAppInstallManifest, ipc, url)
        Response(Status.OK)
      } else {
        Response(Status.NO_CONTENT)
      }
      /*if (request.header("Accept")?.contains("application/json") == true) {
        Response(Status.OK).jsonBody(jmmAppInstallManifest.toJsonElement())
      } else {
        Response(Status.NO_CONTENT)
      }*/
    }
    apiRouting = routes(
      // 安装
      "install" bind Method.GET to routeInstallHandler,
      "/install" bind Method.GET to routeInstallHandler,
      "/uninstall" bind Method.GET to defineBooleanResponse { request ->
        val mmid = queryMmid(request)
        debugJMM("uninstall", mmid)
        jmmMetadataUninstall(mmid)
        true
      },
      "/closeApp" bind Method.GET to defineBooleanResponse { request ->
        val mmid = queryMmid(request)
        jmmController.closeApp(mmid)
        true
      },
      // app详情
      "/detailApp" bind Method.GET to defineBooleanResponse {
        val mmid = queryMmid(request)
        debugJMM("detailApp", mmid)
        val microModule =
          bootstrapContext.dns.query(mmid)

        // TODO: 系统原生应用如WebBrowser的详情页展示？
        if (microModule is JsMicroModule) {
          installJsMicroModule(microModule.metadata, ipc)
          true
        } else {
          false
        }
      },
      "/pause" bind Method.GET to defineBooleanResponse {
        BrowserUIApp.Instance.mBinderService?.invokeUpdateDownloadStatus(
          ipc.remote.mmid, DownLoadController.PAUSE
        )
        true
      },
      /**继续下载*/
      "/resume" bind Method.GET to defineBooleanResponse {
        debugJMM("resume", ipc.remote.mmid)
        BrowserUIApp.Instance.mBinderService?.invokeUpdateDownloadStatus(
          ipc.remote.mmid, DownLoadController.RESUME
        )
        true
      },
      "/cancel" bind Method.GET to defineBooleanResponse {
        debugJMM("cancel", ipc.remote.mmid)
        BrowserUIApp.Instance.mBinderService?.invokeUpdateDownloadStatus(
          ipc.remote.mmid, DownLoadController.CANCEL
        )
        true
      })
  }

  override suspend fun _shutdown() {
  }

  /**
   * 从内存中加载数据
   */
  private fun installJmmApps() {
    ioAsyncScope.launch {
      var preList = mutableListOf<DeskAppInfo>()
      DeskAppInfoStore.queryDeskAppInfoList().collectLatest { list -> // TODO 只要datastore更新，这边就会实时更新
        debugJMM("AppInfoDataStore", "size=${list.size}")
        list.map { deskAppInfo ->
          when (deskAppInfo.appType) {
            AppType.MetaData -> deskAppInfo.metadata?.let { jsMetaData ->
              preList.removeIf { it.metadata?.id == jsMetaData.id }
              // 检测版本
              bootstrapContext.dns.query(jsMetaData.id)?.let { lastMetaData ->
                if (jsMetaData.version.isGreaterThan(lastMetaData.version)) {
                  bootstrapContext.dns.close(lastMetaData.mmid)
                }
              }
              bootstrapContext.dns.install(JsMicroModule(jsMetaData))
            }

            AppType.URL -> deskAppInfo.weblink?.let { deskWebLink ->
              preList.removeIf { preDeskAppInfo -> preDeskAppInfo.weblink?.id == deskWebLink.id }
              bootstrapContext.dns.install(WebLinkMicroModule(deskWebLink))
            }
          }
        }
        /// 将剩余的应用卸载掉
        for (uninstallItem in preList) {
          uninstallItem.weblink?.deleteIconFile() // 删除已下载的图标
          (uninstallItem.metadata?.id ?: uninstallItem.weblink?.id)?.let { uninstallId ->
            bootstrapContext.dns.uninstall(uninstallId)
          }
        }
        preList = list
      }
    }
  }

  private suspend fun installJsMicroModule(
    jmmAppInstallManifest: JmmAppInstallManifest, ipc: Ipc, url: URL? = null,
  ) {
    jmmController.closeSelf() // 如果存在的话，关闭先，同时可以考虑置空
    if (!jmmAppInstallManifest.bundle_url.startsWith("http")) {
      url?.let {
        jmmAppInstallManifest.bundle_url = URL(
          it,
          jmmAppInstallManifest.bundle_url
        ).toString()
      }
    }
    debugJMM("openJmmMetadataInstallPage", jmmAppInstallManifest.bundle_url)
    // 打开安装的界面
    // JmmManagerActivity.startActivity(jmmAppInstallManifest)
    // 打开安装窗口
    val win = createWindowAdapterManager.createWindow(WindowState(
      WindowConstants(
        owner = mmid, ownerVersion = version, provider = mmid, microModule = this
      )
    ).apply {
      mode = WindowMode.MAXIMIZE
    })
    jmmController.bindWindow(win)
    jmmController.installingApps[jmmAppInstallManifest.id] =
      JmmAppInstallController(jmmAppInstallManifest, jmmController)
    jmmController.routeToPath.value = "install-app/${jmmAppInstallManifest.id}"
  }

  private suspend fun jmmMetadataUninstall(mmid: MMID) {
    // 先从列表移除，然后删除文件
    bootstrapContext.dns.uninstall(mmid)
    DeskAppInfoStore.deleteDeskAppInfo(mmid)
    FilesUtil.uninstallApp(App.appContext, mmid)
  }
}
