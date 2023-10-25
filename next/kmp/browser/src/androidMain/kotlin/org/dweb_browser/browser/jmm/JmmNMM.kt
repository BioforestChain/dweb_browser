package org.dweb_browser.browser.jmm

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.bindDwebDeeplink
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.dns.nativeFetchAdaptersManager
import org.dweb_browser.core.std.file.ext.RespondLocalFileContext.Companion.respondLocalFile
import org.dweb_browser.core.sys.dns.returnAndroidFile
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.resolvePath

val debugJMM = Debugger("JMM")

class JmmNMM : NativeMicroModule("jmm.browser.dweb", "Js MicroModule Management") {
  init {
    short_name = "模块管理";
    dweb_deeplinks = listOf("dweb://install")
    categories = listOf(
      MICRO_MODULE_CATEGORY.Application,
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Hub_Service
    );
    icons = listOf(
      ImageResource(
        src = "file:///sys/icons/$mmid.svg", type = "image/svg+xml", purpose = "monochrome"
      )
    )

    /// 提供JsMicroModule的文件适配器
    /// 这个适配器不需要跟着bootstrap声明周期，只要存在JmmNMM模块，就能生效
    nativeFetchAdaptersManager.append { fromMM, request ->
      return@append request.respondLocalFile {
        if (filePath.startsWith("/usr/")) {
          debugJMM("UsrFile", "$fromMM => ${request.href} file=> ${fromMM.mmid}-${fromMM.version}")
          returnAndroidFile(
            nativeFetch("file://file.std.dweb/realPath?path=/data/apps/${fromMM.mmid}-${fromMM.version}").text(),
            filePath
          )
        } else returnNext()
      }
    }
  }

  /**
   * jmm app数据
   */
  private val controllerMap = mutableMapOf<MMID, JsMicroModuleInstallController>()

  fun getApps(mmid: MMID): IMicroModuleManifest? {
    return bootstrapContext.dns.query(mmid)
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val store = JmmStore(this)
    /**
     * 从磁盘中恢复应用
     */
    for (dbItem in store.getAll().values) {
      bootstrapContext.dns.install(JsMicroModule(dbItem.installManifest))
    }

    val routeInstallHandler = defineEmptyResponse {
      val metadataUrl = request.query("url")
      val response = nativeFetch(metadataUrl)
      if (!response.isOk()) {
        throwException(HttpStatusCode.ExpectationFailed, "invalid status code: ${response.status}")
      }
      val jmmAppInstallManifest = response.json<JmmAppInstallManifest>()
      val mmid = jmmAppInstallManifest.id
      debugJMM("listenDownload", "$metadataUrl $mmid")
      openInstaller(jmmAppInstallManifest, metadataUrl)
    }
    routes(
      // 安装
      "install" bindDwebDeeplink routeInstallHandler,
      "/install" bind HttpMethod.Get to routeInstallHandler,
      "/uninstall" bind HttpMethod.Get to defineBooleanResponse {
        val mmid = request.query("app_id")
        debugJMM("uninstall", mmid)
        uninstall(mmid, ipc.remote.version)
        // 从磁盘中移除
        store.delete(mmid)
        true
      },
      // app详情
      "/detail" bind HttpMethod.Get to defineBooleanResponse {
        val mmid = request.query("app_id")
        debugJMM("detailApp", mmid)
        val info = store.get(mmid) ?: return@defineBooleanResponse false
        openInstaller(info.installManifest, info.originUrl)
        true
      })
  }


  private suspend fun openInstaller(
    jmmAppInstallManifest: JmmAppInstallManifest, originUrl: String,
  ) {
    if (!jmmAppInstallManifest.bundle_url.let { it.startsWith("http://") || it.startsWith("https://") }) {
      jmmAppInstallManifest.bundle_url = URLBuilder(originUrl).run {
        resolvePath(jmmAppInstallManifest.bundle_url);
        buildString()
      }
    }
    debugJMM("openJmmMetadataInstallPage", jmmAppInstallManifest.bundle_url)
    val controller = JsMicroModuleInstallController(
      this@JmmNMM, jmmAppInstallManifest
    )
    controller.onDownloadCompleted {
      bootstrapContext.dns.uninstall(jmmAppInstallManifest.id)
      bootstrapContext.dns.install(JsMicroModule(jmmAppInstallManifest))
      JmmStore(this@JmmNMM).set(
        jmmAppInstallManifest.id, JsMicroModuleDBItem(jmmAppInstallManifest, originUrl)
      )
    }
    controller.openRender()
  }

  private suspend fun uninstall(mmid: MMID, version: String) {
    // 在dns中移除app
    bootstrapContext.dns.uninstall(mmid)
    // 在存储中移除文件
    nativeFetch("file://file.std.dweb/remove?path=/data/app/${mmid}-${version}&recursive=true")
  }

  override suspend fun _shutdown() {
  }
}