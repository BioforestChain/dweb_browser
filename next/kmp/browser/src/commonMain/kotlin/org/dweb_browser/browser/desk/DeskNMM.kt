package org.dweb_browser.browser.desk

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.debugBrowser
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.router.IHandlerContext
import org.dweb_browser.core.http.router.ResponseException
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.bindPrefix
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.ext.onActivity
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.ReasonLock
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.getValue
import org.dweb_browser.helper.setValue
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStringBody
import org.dweb_browser.pure.http.initCors
import org.dweb_browser.pure.http.queryAs
import org.dweb_browser.pure.http.queryAsOrNull
import org.dweb_browser.sys.toast.ext.showToast
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.modal.ModalState
import org.dweb_browser.sys.window.core.windowInstancesManager

val debugDesk = Debugger("desk")

class DeskNMM : NativeMicroModule("desk.browser.dweb", "Desk") {
  init {
    name = BrowserI18nResource.Desk.short_name.text
    short_name = BrowserI18nResource.Desk.short_name.text
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Desktop)
    dweb_protocols = listOf("window.sys.dweb", "window.std.dweb", "activity.sys.dweb")
    icons = listOf(
      ImageResource(
        src = "file:///sys/browser-icons/desk.browser.dweb.svg",
        type = "image/svg+xml",
        // purpose = "monochrome"
      )
    )
  }

  companion object {
    internal val controllersMap = mutableMapOf<String, DeskController>()
  }

  inner class DeskRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    val runningAppsFlow = MutableStateFlow(mapOf<MMID, RunningApp>())
    var runningApps by runningAppsFlow

    /**
     * 将ipc作为Application实例进行打开
     */
    private suspend fun getRunningApp(ipc: Ipc): RunningApp? {
      val mmid = ipc.remote.mmid
      /// 如果成功打开，将它“追加”到列表中
      return when (val runningApp = runningApps[mmid]) {
        null -> {
          if (ipc.remote.categories.contains(MICRO_MODULE_CATEGORY.Application)) {
            RunningApp(ipc, bootstrapContext).also { app ->
              runningApps += mmid to app
              /// 如果应用关闭，将它从列表中移除
              app.ipc.onClosed {
                runningApps -= mmid
              }
            }
          } else null
        }

        else -> runningApp
      }
    }

    private suspend fun IHandlerContext.getRunningApp(ipc: Ipc) = openAppLock.withLock("app") {
      this@DeskRuntime.getRunningApp(ipc) ?: throwException(
        HttpStatusCode.NotFound, "microModule(${ipc.remote.mmid}) is not an application"
      )
    }

    private val openAppLock = ReasonLock()
    suspend fun IHandlerContext.openOrActivateAppWindow(
      ipc: Ipc, desktopController: DesktopControllerBase,
    ): WindowController {
      val appId = ipc.remote.mmid
      debugDesk("ActivateAppWindow", appId)
      try {
        /// desk直接为应用打开窗口，因为窗口由desk统一管理，所以由desk窗口，并提供句柄
        val appMainWindow = getAppMainWindow(ipc)
        /// 将所有的窗口聚焦
        desktopController.getDesktopWindowsManager().focusWindow(appId)
        return appMainWindow
      } catch (e: Exception) {
        deskController.alertController.showAlert(e)
        e.printStackTrace()
        throwException(cause = e)
      }
    }

    suspend fun IHandlerContext.getAppMainWindow(ipc: Ipc = this.ipc) =
      openAppLock.withLock("window") {
        getWindow {
          val runningApp = getRunningApp(ipc)
          /// desk直接为应用打开窗口，因为窗口由desk统一管理，所以由desk窗口，并提供句柄
          runningApp.tryOpenMainWindow()
        }
      }

    suspend fun IHandlerContext.createModal(ipc: Ipc) = openAppLock.withLock("write-modal") {
      request.queryAs<ModalState>().also {
        saveAndTryOpenModal(ipc, it)
      }
    }

    private suspend fun IHandlerContext.saveAndTryOpenModal(
      ipc: Ipc,
      modal: ModalState,
    ) {
      val appMainWindow = getAppMainWindow(ipc)
      appMainWindow.saveModal(modal)
      if (request.queryAsOrNull<Boolean>("open") == true) {
        appMainWindow.openModal(modal.modalId)
      }
    }

    suspend fun IHandlerContext.getWindow(orElse: (suspend () -> WindowController)? = null) =
      request.queryOrNull("wid")?.let { wid ->
        windowInstancesManager.get(wid) ?: throw ResponseException(
          code = HttpStatusCode.NotFound, message = "No Found Window by wid: $wid"
        )
      } ?: orElse?.invoke() ?: throw ResponseException(
        code = HttpStatusCode.ExpectationFailed, message = "Fail To Get Window"
      )

    val deskController = DeskController(this)

    override suspend fun _bootstrap() {
      controllersMap[deskController.sessionId] = deskController
      onShutdown {
        controllersMap.remove(deskController.sessionId)
      }

      /// 窗口协议
      windowProtocol()

      /// 实时活动协议
      activityProtocol()

      /// 内部接口
      routes(
        //
        "/readFile" bind PureMethod.GET by definePureResponse {
          nativeFetch(request.query("url"))
        },
        // readAccept
        "/readAccept." bindPrefix PureMethod.GET by definePureResponse {
          return@definePureResponse PureResponse(
            HttpStatusCode.OK,
            body = PureStringBody("""{"accept":"${request.headers.get("Accept")}"}""")
          )
        },
        // 关闭app
        "/closeApp" bind PureMethod.GET by defineBooleanResponse {
          openAppLock.withLock("app") {
            val mmid = request.query("app_id")
            when (val runningApp = runningApps[mmid]) {
              null -> false
              else -> {
                runningApp.closeMainWindow();
                true
              }
            }
          }
        },
        "/showToast" bind PureMethod.GET by defineEmptyResponse {
          debugBrowser("showToast", request.href)
          val message = request.query("message")
          showToast(message)
        },
        "/proxy" bind PureMethod.GET by definePureResponse {
          val url = request.query("url")
          nativeFetch(url).also {
            it.headers.initCors()
          }
        }
      ).cors()

      onActivity { startDeskView(deskController.sessionId) }
      /// 启动桌面视图
      coroutineScope { startDeskView(deskController.sessionId) }
      /// 等待主视图启动完成
      deskController.awaitReady()
    }

    override suspend fun _shutdown() {
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = DeskRuntime(bootstrapContext)
}

expect suspend fun DeskNMM.DeskRuntime.startDeskView(deskSessionId: String)

/**打开或激活app*/
internal suspend fun NativeMicroModule.NativeRuntime.openAppOrActivate(mmid: MMID) {
  nativeFetch(buildUrlString("file://desk.browser.dweb/openAppOrActivate") {
    parameters["app_id"] = mmid
  })
}

/**关闭app*/
internal suspend fun NativeMicroModule.NativeRuntime.closeApp(mmid: MMID) {
  nativeFetch(buildUrlString("file://desk.browser.dweb/closeApp") {
    parameters["app_id"] = mmid
  })
}