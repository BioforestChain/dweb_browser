package org.dweb_browser.browser.jmm

import io.ktor.http.HttpStatusCode
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.toast.ext.showToast
import org.dweb_browser.sys.window.core.WindowRenderProvider
import org.dweb_browser.sys.window.ext.createBottomSheets
import org.dweb_browser.sys.window.ext.getOrOpenMainWindowId
import org.dweb_browser.sys.window.ext.getWindow
import org.dweb_browser.sys.window.ext.onRenderer

val debugJMM = Debugger("JMM")

class JmmNMM :
  NativeMicroModule("jmm.browser.dweb", "Js MicroModule Service") {
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
    dweb_deeplinks = listOf("dweb://install")
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Hub_Service)
    /// 提供JsMicroModule的文件适配器
    /// 这个适配器不需要跟着bootstrap声明周期，只要存在JmmNMM模块，就能生效
    nativeFetchAdaptersManager.append { fromMM, request ->
      val usrRootMap = mutableMapOf<String, Deferred<String>>()
      return@append request.respondLocalFile {
        if (filePath.startsWith("/usr/")) {
          debugJMM("UsrFile", "$fromMM => ${request.href}")
          val root = usrRootMap.getOrPut(fromMM.mmid) {
            fromMM.ioAsyncScope.async {
              this@JmmNMM.withBootstrap {
                this@JmmNMM.nativeFetch("file://file.std.dweb/realPath?path=/data/apps/${fromMM.mmid}")
                  .text()
              }
            }
          }.await()
          returnFile(root, filePath)
        } else returnNext()
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    bootstrapContext.dns.install(JmmGuiNMM())

    val store = JmmStore(this)
    loadJmmAppList(store) // 加载安装的应用信息

    val jmmController = JmmController(this, store)
    jmmController.loadHistoryMetadataUrl() // 加载之前加载过的应用

    /// 注册子协议
//    this.protocol("file.std.dweb") {
//      this.onConnect { (clientIpc) ->
//        println("xxxxx=> ${clientIpc.remote.mmid}")
//        val fileSubIpc = connect("file.std.dweb")
//        clientIpc.pipe(fileSubIpc)
//        fileSubIpc.pipe(clientIpc)
//      }
//    }

    val routeInstallHandler = defineEmptyResponse {
      val metadataUrl = request.query("url")
      coroutineScope {
        // 加载url资源，这一步可能要多一些时间
        val response = nativeFetch(metadataUrl)
        if (!response.isOk) {
          val message = "invalid status code: ${response.status}"
          showToast(message)
          throwException(HttpStatusCode.ExpectationFailed, message)
        }
        // 打开渲染器
        launch {
          jmmController.openOrUpsetInstallerView(metadataUrl)
        }
        val jmmAppInstallManifest = response.json<JmmAppInstallManifest>()
        debugJMM("listenDownload", "$metadataUrl ${jmmAppInstallManifest.id}")
        jmmController.openOrUpsetInstallerView(metadataUrl, jmmAppInstallManifest)
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
        jmmController.openOrUpsetInstallerView(info.originUrl, info.installManifest)
        true
      }).cors()

    routes(
      /// 收到wid
      "/renderer" bind PureMethod.GET by defineEmptyResponse {
        val wid = request.query("wid")
        widDeferredAtomic.update { old ->
          when {
            old.isCompleted -> CompletableDeferred()
            else -> old
          }.also { new ->
            new.complete(wid)
          }
        }
        /// 在IPC关闭的时候，销毁wid
        ipc.onClose {
          widDeferredAtomic.update { old ->
            when {
              /// 如果原本的 renderer 已经完成并且指定的wid对得上号，那么有权销毁并提供一个新的
              old.isCompleted && old.getCompleted() == wid -> {
                CompletableDeferred()
              }
              // 否则，保持原本
              else -> old
            }
          }
        }
        jmmController.openHistoryView()
      }).protected("gui.jmm.browser.dweb")
  }

  private val widDeferredAtomic = atomic(CompletableDeferred<String>())
  suspend fun getMainWindowId() = widDeferredAtomic.value.await()
  suspend fun getMainWindow() = getWindow(getMainWindowId())
  val hasMainWindow get() = widDeferredAtomic.value.isCompleted

  private suspend fun openMainWindow() = getWindow(
    nativeFetch("file://gui.jmm.browser.dweb/openMainWindow").text()
  )

  private suspend fun getOrOpenMainWindowId() =
    if (!hasMainWindow) openMainWindow().id else getMainWindowId()

  suspend fun getOrOpenMainWindow() = getWindow(getOrOpenMainWindowId())

  suspend fun createBottomSheets(
    title: String? = null,
    iconUrl: String? = null,
    iconAlt: String? = null,
    renderProvider: WindowRenderProvider,
  ) = (this as NativeMicroModule).createBottomSheets(
    title,
    iconUrl,
    iconAlt,
    wid = getOrOpenMainWindowId(),
    renderProvider = renderProvider
  )

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

class JmmGuiNMM : NativeMicroModule("gui.jmm.browser.dweb", "Js MicroModule Management") {
  init {
    short_name = BrowserI18nResource.jmm_short_name.text;
    categories = listOf(MICRO_MODULE_CATEGORY.Application)
    icons = listOf(
      ImageResource(
        src = "file:///sys/icons/jmm.browser.dweb.svg",
        type = "image/svg+xml",
        purpose = "monochrome"
      )
    )
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      "/openMainWindow" bind PureMethod.GET by defineStringResponse {
        getOrOpenMainWindowId()
      },
    ).protected("jmm.browser.dweb");
    onRenderer {
      nativeFetch(PureMethod.GET, "file://jmm.browser.dweb/renderer?wid=$wid")
    }
  }


  override suspend fun _shutdown() {
  }
}