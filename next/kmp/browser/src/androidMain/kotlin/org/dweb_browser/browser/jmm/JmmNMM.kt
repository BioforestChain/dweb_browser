package org.dweb_browser.browser.jmm

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import kotlinx.coroutines.launch
import org.dweb_browser.browser.link.WebLinkMicroModule
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.bindDwebDeeplink
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.dns.nativeFetchAdaptersManager
import org.dweb_browser.core.std.file.ext.RespondLocalFileContext.Companion.respondLocalFile
import org.dweb_browser.core.sys.dns.returnAndroidFile
import org.dweb_browser.core.sys.download.JmmDownloadInfo
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.isGreaterThan
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
  private val downloadingApp = ChangeableMap<MMID, JmmDownloadInfo>() // 正在下载的列表

  fun getApps(mmid: MMID): IMicroModuleManifest? {
    return bootstrapContext.dns.query(mmid)
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val store = JmmStore(this)
    installJmmApps(store)

    val routeInstallHandler = definePureResponse {
      val metadataUrl = request.query("url")
      val response = nativeFetch(metadataUrl)
      if (response.isOk()) {
        try {
          val jmmAppInstallManifest = response.json<JmmAppInstallManifest>()
          debugJMM("listenDownload", "$metadataUrl ${jmmAppInstallManifest.id}")
          val url = Url(metadataUrl)
          // 根据 jmmMetadata 打开一个应用信息的界面，用户阅读界面信息后，可以点击"安装"
          installJsMicroModule(jmmAppInstallManifest, url)
          PureResponse(HttpStatusCode.OK)
        } catch (e: Throwable) {
          e.printStackTrace()
          debugJMM("install", "fail -> ${e.message}")
          PureResponse(HttpStatusCode.ExpectationFailed).body(e.stackTraceToString())
        }
      } else {
        PureResponse(HttpStatusCode.ExpectationFailed).body("invalid status code: ${response.status}")
      }
    }
    routes(
      // 安装
      "install" bindDwebDeeplink routeInstallHandler,
      "/install" bind HttpMethod.Get to routeInstallHandler,
      "/uninstall" bind HttpMethod.Get to defineBooleanResponse {
        val mmid = request.query("app_id")
        debugJMM("uninstall", mmid)
        uninstall(mmid, ipc.remote.version)
        // 在缓存中移除
        store.delete(mmid)
        true
      },
      // app详情
      "/detailApp" bind HttpMethod.Get to defineBooleanResponse {
        val mmid = request.query("app_id")
        debugJMM("detailApp", mmid)
        val microModule = bootstrapContext.dns.query(mmid)
        if (microModule is JsMicroModule) {
          installJsMicroModule(microModule.metadata)
          true
        } else {
          false
        }
      })
  }

  override suspend fun _shutdown() {
  }

  /**
   * 从内存中加载数据
   */
  private fun installJmmApps(store: JmmStore) {
    ioAsyncScope.launch {
      store.getAll().map { (key, deskAppInfo) ->
        when (deskAppInfo.appType) {
          AppType.Jmm -> deskAppInfo.metadata?.let { jsMetaData ->
            // 检测版本
            bootstrapContext.dns.query(jsMetaData.id)?.let { lastMetaData ->
              if (jsMetaData.version.isGreaterThan(lastMetaData.version)) {
                bootstrapContext.dns.close(lastMetaData.mmid)
              }
            }
            bootstrapContext.dns.install(JsMicroModule(jsMetaData))
          }

          AppType.Link -> deskAppInfo.weblink?.let { deskWebLink ->
            bootstrapContext.dns.install(
              WebLinkMicroModule(
                deskWebLink
              )
            )
          }

          else -> {}
        }
      }
    }
  }

  private fun installJsMicroModule(
    jmmAppInstallManifest: JmmAppInstallManifest, url: Url? = null,
  ): JmmController {
    if (!jmmAppInstallManifest.bundle_url.startsWith("http")) {
      url?.let {
        jmmAppInstallManifest.bundle_url = URLBuilder(it).run {
          resolvePath(jmmAppInstallManifest.bundle_url);
          buildString()
        }
      }
    }
    debugJMM("openJmmMetadataInstallPage", jmmAppInstallManifest.bundle_url)
    return JmmController(
      this@JmmNMM, jmmAppInstallManifest
    )
  }

  private suspend fun uninstall(mmid: MMID, version: String) {
    // 在dns中移除app
    bootstrapContext.dns.uninstall(mmid)
    // 在存储中移除文件
    nativeFetch("file://file.std.dweb/remove?path=/data/app/${mmid}-${version}&recursive=true")
  }
}