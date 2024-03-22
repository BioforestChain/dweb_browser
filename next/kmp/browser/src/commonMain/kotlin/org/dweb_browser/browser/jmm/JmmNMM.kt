package org.dweb_browser.browser.jmm

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.bindDwebDeeplink
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.dns.nativeFetchAdaptersManager
import org.dweb_browser.core.std.file.ext.RespondLocalFileContext.Companion.respondLocalFile
import org.dweb_browser.core.std.file.ext.realFile
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.toast.ext.showToast
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.getOrOpenMainWindow
import org.dweb_browser.sys.window.ext.onRenderer

val debugJMM = Debugger("JMM")

class JmmNMM : NativeMicroModule("jmm.browser.dweb", "Js MicroModule Service") {
  companion object {
    init {
      IDWebView.Companion.brands.add(
        IDWebView.UserAgentBrandData(
          "jmm.browser.dweb",
          "${JsMicroModule.VERSION}.${JsMicroModule.PATCH}"
        )
      )
    }
  }

  init {
    short_name = BrowserI18nResource.jmm_short_name.text
    categories = listOf(
      MICRO_MODULE_CATEGORY.Application,
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Hub_Service
    )
    icons = listOf(
      ImageResource(
        src = "file:///sys/icons/jmm.browser.dweb.svg",
        type = "image/svg+xml",
        // purpose = "monochrome"
      )
    )
    dweb_deeplinks = listOf("dweb://install")
    /// 提供JsMicroModule的文件适配器
    /// 这个适配器不需要跟着bootstrap声明周期，只要存在JmmNMM模块，就能生效
    nativeFetchAdaptersManager.append { fromMM, request ->
      val usrRootMap = mutableMapOf<String, Deferred<String>>()
      return@append request.respondLocalFile {
        if (filePath.startsWith("/usr/")) {
          val rootKey = "${fromMM.mmid}-${fromMM.version}"
          debugJMM("UsrFile", "$fromMM => ${request.href} in $rootKey")
          val root = usrRootMap.getOrPut(fromMM.mmid) {
            fromMM.ioAsyncScope.async {
              this@JmmNMM.withBootstrap {
                this@JmmNMM.realFile("/data/apps/${fromMM.mmid}-${fromMM.version}")
              }
            }
          }.await()
          debugJMM("respondLocalFile", root)
          returnFile(root, filePath)
        } else returnNext()
      }
    }
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val store = JmmStore(this)
    loadJmmAppList(store) // 加载安装的应用信息

    val jmmController = JmmController(this, store)
    jmmController.loadHistoryMetadataUrl() // 加载之前加载过的应用

    val routeInstallHandler = defineEmptyResponse {
      val metadataUrl = request.query("url")
      // 打开渲染器
      coroutineScope {
        val job = launch {
          jmmController.openOrUpsetInstallerView(metadataUrl)
          // 超时请求的一些操作
          delay(5000)
          showToast("请求超时，请检查下载链接")
          getOrOpenMainWindow().closeRoot()
        }
        // 加载url资源，这一步可能要多一些时间
        val response = nativeFetch(metadataUrl)
        job.cancel()
        if (!response.isOk) {
          val message = "invalid status code: ${response.status}"
          showToast(message)
          throwException(HttpStatusCode.ExpectationFailed, message)
        }

        val jmmAppInstallManifest = response.json<JmmAppInstallManifest>()
        debugJMM("listenDownload", "$metadataUrl ${jmmAppInstallManifest.id}")
        jmmController.openOrUpsetInstallerView(
          metadataUrl, jmmAppInstallManifest.createJmmHistoryMetadata(metadataUrl)
        )
      }
    }
    routes(
      // 安装
      "install" bindDwebDeeplink routeInstallHandler,
      "/install" bind PureMethod.GET by routeInstallHandler,
      "/uninstall" bind PureMethod.GET by defineBooleanResponse {
        val mmid = request.query("app_id")
        debugJMM("uninstall", "mmid=$mmid")
        jmmController.uninstall(mmid)
      },
      // app详情
      "/detail" bind PureMethod.GET by defineBooleanResponse {
        val mmid = request.query("app_id")
        debugJMM("detailApp", mmid)
        val info = store.getApp(mmid) ?: return@defineBooleanResponse false
        jmmController.openOrUpsetInstallerView(
          info.originUrl, info.installManifest.createJmmHistoryMetadata(info.originUrl)
        )
        true
      }).cors()

    onRenderer {
      getMainWindow().apply {
        setStateFromManifest(this@JmmNMM)
        state.keepBackground = true/// 保持在后台运行
        jmmController.openHistoryView(this)
      }
    }
  }


  /**
   * 从磁盘中恢复应用
   */
  private suspend fun loadJmmAppList(store: JmmStore) {
    for (dbItem in store.getAllApps().values) {
      bootstrapContext.dns.install(JsMicroModule(dbItem.installManifest))
    }
  }

  override suspend fun _shutdown() {
  }
}
