package org.dweb_browser.browser.jmm

import okio.FileSystem
import okio.Path
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.bindDwebDeeplink
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.IVirtualFsDirectory
import org.dweb_browser.core.std.file.ext.realPath
import org.dweb_browser.core.std.file.fileTypeAdapterManager
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.removeInvisibleChars
import org.dweb_browser.helper.removeWhen
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.io.SystemFileSystem
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer
import org.dweb_browser.sys.window.ext.openMainWindow

val debugJMM = Debugger("JMM")

class JmmNMM : NativeMicroModule("jmm.browser.dweb", "Js MicroModule Service") {
  companion object {
    init {
      IDWebView.Companion.brands.add(
        IDWebView.UserAgentBrandData(
          "jmm.browser.dweb",
          "${JsMicroModule.VERSION}",
          "${JsMicroModule.VERSION}.${JsMicroModule.PATCH}"
        )
      )
    }
  }

  init {
    name = BrowserI18nResource.JMM.short_name.text
    short_name = BrowserI18nResource.JMM.short_name.text
    categories = listOf(
      MICRO_MODULE_CATEGORY.Application,
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Hub_Service
    )
    icons = listOf(
      ImageResource(
        src = "file:///sys/browser-icons/jmm.browser.dweb.svg",
        type = "image/svg+xml",
        // purpose = "monochrome"
      )
    )
    dweb_deeplinks = listOf("dweb://install")
  }

  inner class JmmRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {

    override suspend fun _bootstrap() {
      val store = JmmStore(this)
      loadJmmAppList(store) // 加载安装的应用信息

      val jmmController = JmmController(this, store)
      jmmController.loadHistoryMetadataUrl() // 加载之前加载过的应用

      val routeInstallHandler = defineEmptyResponse {
        val metadataUrl = request.query("url").removeInvisibleChars().trim()
        val referrerUrl = request.headers.getOrNull("referrer")

        debugJMM("fetchJmmMetadata", metadataUrl)
        // 加载url资源，这一步可能要多一些时间
        val jmmMetadata = jmmController.fetchJmmMetadata(metadataUrl, referrerUrl)
        jmmController.openInstallerView(jmmMetadata)
      }

      /// 提供JsMicroModule的文件适配器
      /// file:///usr/*
      val appsDir = realPath("/data/apps")
      val usr = object : IVirtualFsDirectory {
        override fun isMatch(firstSegment: String) = firstSegment == "usr"
        override val fs: FileSystem = SystemFileSystem
        override fun resolveTo(remote: IMicroModuleManifest, virtualFullPath: Path) =
          appsDir.resolve("${remote.mmid}-${remote.version}${virtualFullPath}")
      }
      fileTypeAdapterManager.append(adapter = usr).removeWhen(mmScope)

      /// 服务
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
          jmmController.openDetailView(info.jmmMetadata)
          openMainWindow()
          true
        },
        // 是否有安装
        "/isInstalled" bind PureMethod.GET by defineBooleanResponse {
          val mmid = request.query("app_id")
          debugJMM("isInstalled", mmid)
          store.getApp(mmid) != null
        },

        ).cors()

      onRenderer {
        getMainWindow().apply {
          setStateFromManifest(manifest)
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

  override fun createRuntime(bootstrapContext: BootstrapContext) = JmmRuntime(bootstrapContext)
}
