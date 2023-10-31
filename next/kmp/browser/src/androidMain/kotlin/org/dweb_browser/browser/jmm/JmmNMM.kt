package org.dweb_browser.browser.jmm

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import org.dweb_browser.browser.download.TaskId
import org.dweb_browser.browser.jmm.ui.JmmStatus
import org.dweb_browser.browser.web.ui.browser.model.isUrl
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
import org.dweb_browser.helper.isGreaterThan
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.sys.window.core.helper.setFromManifest
import org.dweb_browser.sys.window.ext.getMainWindow
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

  // jmm app数据
  private val controllerMap = mutableMapOf<MMID, JmmInstallerController>()

  // jmm download数据
  private val downloadTaskIdMap = mutableMapOf<MMID, String>()

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val store = JmmStore(this)
    loadJmmAppList(store) // 加载安装的应用信息
    loadJmmDownloadList(store) // 加载下载的信息

    val routeInstallHandler = defineEmptyResponse {
      val metadataUrl = request.query("url")
      val response = nativeFetch(metadataUrl)
      if (!response.isOk()) {
        throwException(HttpStatusCode.ExpectationFailed, "invalid status code: ${response.status}")
      }
      val jmmAppInstallManifest = response.json<JmmAppInstallManifest>()
      debugJMM("listenDownload", "$metadataUrl ${jmmAppInstallManifest.id}")
      openInstallerView(jmmAppInstallManifest, metadataUrl, store)
    }
    routes(
      // 安装
      "install" bindDwebDeeplink routeInstallHandler,
      "/install" bind HttpMethod.Get to routeInstallHandler,
      "/uninstall" bind HttpMethod.Get to defineBooleanResponse {
        val mmid = request.query("app_id")
        val data = store.getApp(mmid) ?: return@defineBooleanResponse false
        val installMetadata = data.installManifest
        debugJMM("uninstall", "$mmid-${installMetadata.bundle_url} ${installMetadata.version} ")
        uninstall(mmid, installMetadata.version)
        // 从磁盘中移除整个
        store.deleteApp(mmid)
        val taskId = store.getTaskId(mmid)
        store.deleteJMMTaskId(mmid)
        // 从磁盘中移除下载记录
        if (taskId !== null) {
          removeTask(taskId)
        }
        true
      },
      // app详情
      "/detail" bind HttpMethod.Get to defineBooleanResponse {
        val mmid = request.query("app_id")
        debugJMM("detailApp", mmid)
        val info = store.getApp(mmid) ?: return@defineBooleanResponse false
        openInstallerView(info.installManifest, info.originUrl, store)
        true
      })
    onRenderer {
      getMainWindow().state.setFromManifest(this@JmmNMM)
    }
  }

  /**
   * 打开安装器视图
   */
  private suspend fun openInstallerView(
    jmmAppInstallManifest: JmmAppInstallManifest, originUrl: String, store: JmmStore
  ) {
    if (!jmmAppInstallManifest.bundle_url.let { it.isUrl() }) {
      jmmAppInstallManifest.bundle_url = URLBuilder(originUrl).run {
        resolvePath(jmmAppInstallManifest.bundle_url)
        buildString()
      }
    }
    debugJMM("openInstallerView", jmmAppInstallManifest.bundle_url)
    val controller = controllerMap.getOrPut(jmmAppInstallManifest.id) {
      JmmInstallerController(
        this@JmmNMM, jmmAppInstallManifest, originUrl, downloadTaskIdMap[jmmAppInstallManifest.id]
      ).also { controller ->
        controller.onJmmStateListener { pair ->
          when (pair.first) {
            JmmStatus.Init -> {
              store.saveJMMTaskId(jmmAppInstallManifest.id, pair.second)
            }

            JmmStatus.Completed -> {
              // 安装完成后，删除该数据的下载记录，避免状态出现问题
              downloadTaskIdMap.remove(jmmAppInstallManifest.id)
              store.deleteJMMTaskId(jmmAppInstallManifest.id)
            }

            JmmStatus.INSTALLED -> {
              // 安装完成，卸载之前的，安装新的
              bootstrapContext.dns.uninstall(jmmAppInstallManifest.id)
              bootstrapContext.dns.install(JsMicroModule(jmmAppInstallManifest))
              // 存储app信息到内存
              store.setApp(
                jmmAppInstallManifest.id, JsMicroModuleDBItem(jmmAppInstallManifest, originUrl)
              )
            }

            else -> null
          }
        }
      }
    }
    // 打开渲染
    controller.openRender(
      jmmAppInstallManifest.version.isGreaterThan(controller.jmmAppInstallManifest.version)
    )
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
        "file://file.std.dweb/remove?path=${filepath}&recursive=true", IpcMethod.DELETE
      )
    ).boolean()
  }

  private suspend fun removeTask(taskId: TaskId): Boolean {
    return nativeFetch(
      PureRequest(
        "file://download.browser.dweb/remove?taskId=${taskId}", IpcMethod.DELETE
      )
    ).boolean()
  }

  /**
   * 从磁盘中恢复应用
   */
  private suspend fun loadJmmAppList(store: JmmStore) {
    for (dbItem in store.getAllApps().values) {
      bootstrapContext.dns.install(JsMicroModule(dbItem.installManifest))
    }
  }

  /**
   * 恢复下载任务
   */
  private suspend fun loadJmmDownloadList(store: JmmStore) {
    store.getAllJMMTaskId().map { (key, taskId) ->
      debugJMM("loadJmmDownloadList", "恢复:$key $taskId")
      downloadTaskIdMap.put(key, taskId)
    }
  }

  override suspend fun _shutdown() {
  }
}