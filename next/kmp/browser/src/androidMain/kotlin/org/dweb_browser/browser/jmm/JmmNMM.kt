package org.dweb_browser.browser.jmm

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.bindDwebDeeplink
import org.dweb_browser.core.ipc.helper.IpcMethod
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
  private val controllerMap = mutableMapOf<MMID, JmmInstallerController>()

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
      openInstallerView(jmmAppInstallManifest, metadataUrl)
    }
    routes(
      // 安装
      "install" bindDwebDeeplink routeInstallHandler,
      "/install" bind HttpMethod.Get to routeInstallHandler,
      "/uninstall" bind HttpMethod.Get to defineBooleanResponse {
        val mmid = request.query("app_id")
        val data = store.get(mmid) ?: return@defineBooleanResponse  false
        val installMetadata = data.installManifest
        debugJMM("uninstall", "$mmid-${installMetadata.bundle_url} ${installMetadata.version} ")
        uninstall(mmid, installMetadata.version)
       // 从磁盘中移除
        store.delete(mmid)
        true
      },
      // app详情
      "/detail" bind HttpMethod.Get to defineBooleanResponse {
        val mmid = request.query("app_id")
        debugJMM("detailApp", mmid)
        val info = store.get(mmid) ?: return@defineBooleanResponse false
        openInstallerView(info.installManifest, info.originUrl)
        true
      })
  }

  /**
   * 打开安装器视图
   */
  private suspend fun openInstallerView(
    jmmAppInstallManifest: JmmAppInstallManifest, originUrl: String,
  ) {
    if (!jmmAppInstallManifest.bundle_url.let { it.startsWith("http://") || it.startsWith("https://") }) {
      jmmAppInstallManifest.bundle_url = URLBuilder(originUrl).run {
        resolvePath(jmmAppInstallManifest.bundle_url);
        buildString()
      }
    }
    debugJMM("openJmmMetadataInstallPage", jmmAppInstallManifest.bundle_url)
    val controller = controllerMap.getOrPut(jmmAppInstallManifest.id) {
      JmmInstallerController(
        this@JmmNMM, jmmAppInstallManifest
      ).also {
        it.onDownloadCompleted {
          bootstrapContext.dns.uninstall(jmmAppInstallManifest.id)
          bootstrapContext.dns.install(JsMicroModule(jmmAppInstallManifest))
          JmmStore(this@JmmNMM).set(
            jmmAppInstallManifest.id, JsMicroModuleDBItem(jmmAppInstallManifest, originUrl)
          )
        }
      }
    }
    // 打开渲染
    controller.openRender()
  }

  private suspend fun uninstall(mmid: MMID, version: String) {
    // 在dns中移除app
    bootstrapContext.dns.uninstall(mmid)
    // 在存储中移除整个app
    remove("/data/apps/${mmid}-${version}")
  }

  suspend fun remove(filepath: String): Boolean {
    return nativeFetch(
      PureRequest(
        "file://file.std.dweb/remove?path=${filepath}&recursive=true",
        IpcMethod.DELETE
      )
    ).boolean()
  }

  override suspend fun _shutdown() {
  }
}