package org.dweb_browser.browser.jmm

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.bindDwebDeeplink
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.dns.nativeFetchAdaptersManager
import org.dweb_browser.core.std.file.ext.RespondLocalFileContext.Companion.respondLocalFile
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.sys.window.ext.onRenderer

val debugJMM = Debugger("JMM")

class JmmNMM : NativeMicroModule("jmm.browser.dweb", "Js MicroModule Management") {
  init {
    short_name = "模块管理";
    dweb_deeplinks = listOf("dweb://install")
    categories = listOf(
      MICRO_MODULE_CATEGORY.Application,
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Hub_Service
    )
    icons = listOf(
      ImageResource(
        src = "file:///sys/icons/$mmid.svg", type = "image/svg+xml", purpose = "monochrome"
      )
    )

    /// 提供JsMicroModule的文件适配器
    /// 这个适配器不需要跟着bootstrap声明周期，只要存在JmmNMM模块，就能生效
    nativeFetchAdaptersManager.append { fromMM, request ->
      val usrRootMap = mutableMapOf<String, Deferred<String>>()
      return@append request.respondLocalFile {
        if (filePath.startsWith("/usr/")) {
          val rootKey = "${fromMM.mmid}-${fromMM.version}"
          debugJMM("UsrFile", "$fromMM => ${request.href} in $rootKey")
          val root = usrRootMap.getOrPut(rootKey) {
            fromMM.ioAsyncScope.async {
              this@JmmNMM.nativeFetch("file://file.std.dweb/realPath?path=/data/apps/${fromMM.mmid}-${fromMM.version}")
                .text()
            }
          }.await()
          returnFile(root, filePath)
        } else returnNext()
      }
    }
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val store = JmmStore(this)
    val jmmController = JmmController(this, store)
    loadJmmAppList(store) // 加载安装的应用信息
    jmmController.loadHistoryMetadataUrl() // 加载之前加载过的应用

    val routeInstallHandler = defineEmptyResponse {
      val metadataUrl = request.query("url")
      val response = nativeFetch(metadataUrl)
      if (!response.isOk()) {
        throwException(HttpStatusCode.ExpectationFailed, "invalid status code: ${response.status}")
      }
      val jmmAppInstallManifest = response.json<JmmAppInstallManifest>()
      debugJMM("listenDownload", "$metadataUrl ${jmmAppInstallManifest.id}")
      jmmController.openInstallerView(jmmAppInstallManifest, metadataUrl)
    }
    routes(
      // 安装
      "install" bindDwebDeeplink routeInstallHandler,
      "/install" bind HttpMethod.Get by routeInstallHandler,
      "/uninstall" bind HttpMethod.Get by defineBooleanResponse {
        val mmid = request.query("app_id")
        val data = store.getApp(mmid) ?: return@defineBooleanResponse false
        val installMetadata = data.installManifest
        debugJMM("uninstall", "$mmid-${installMetadata.bundle_url} ${installMetadata.version} ")
        jmmController.uninstall(mmid, installMetadata.version)
        // 从磁盘中移除整个
        store.deleteApp(mmid)
        true
      },
      // app详情
      "/detail" bind HttpMethod.Get by defineBooleanResponse {
        val mmid = request.query("app_id")
        debugJMM("detailApp", mmid)
        val info = store.getApp(mmid) ?: return@defineBooleanResponse false
        jmmController.openInstallerView(info.installManifest, info.originUrl)
        true
      })
    onRenderer {
      jmmController.openHistoryView()
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