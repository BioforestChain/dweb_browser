package info.bagen.dwebbrowser.microService.browser.desk

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.PureStringBody
import org.dweb_browser.core.http.router.IHandlerContext
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.bindPrefix
import org.dweb_browser.core.http.toPure
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.startAppActivity
import org.dweb_browser.core.std.dns.ext.onActivity
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.http.CORS_HEADERS
import org.dweb_browser.core.std.http.DwebHttpServerOptions
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.core.std.http.createHttpDwebServer
import org.dweb_browser.helper.ChangeState
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.ReasonLock
import org.dweb_browser.helper.consumeEachJsonLine
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.sys.window.core.ModalState
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.windowInstancesManager

val debugDesk = Debugger("desk")

class DeskNMM : NativeMicroModule("desk.browser.dweb", "Desk") {
  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Desktop);
    dweb_protocols = listOf("window.sys.dweb")
  }

  private val runningApps = ChangeableMap<MMID, RunningApp>()

  /**
   * 将ipc作为Application实例进行打开
   */
  private fun getRunningApp(ipc: Ipc): RunningApp? {
    val mmid = ipc.remote.mmid
    /// 如果成功打开，将它“追加”到列表中
    return when (val runningApp = runningApps[mmid]) {
      null -> {
        if (ipc.remote.categories.contains(MICRO_MODULE_CATEGORY.Application)) {
          RunningApp(ipc, this, bootstrapContext).also {
            runningApps[mmid] = it
            /// 如果应用关闭，将它从列表中移除
            it.onClose {
              runningApps.remove(mmid)
            }
          }
        } else null
      }

      else -> runningApp
    }
  }

  companion object {
    data class DeskControllers(
      val desktopController: DesktopController,
      val taskbarController: TaskbarController,
      val deskNMM: DeskNMM,
    ) {
      val activityPo = PromiseOut<Activity>()
    }

    val controllersMap = mutableMapOf<String, DeskControllers>()
  }

  private suspend fun listenApps() = ioAsyncScope.launch {
    val (openedAppIpc) = bootstrapContext.dns.connect("dns.std.dweb")
    suspend fun doObserve(urlPath: String, cb: suspend ChangeState<MMID>.() -> Unit) {
      val res = openedAppIpc.request(urlPath)
      res.stream().getReader("Desk listenApps").consumeEachJsonLine<ChangeState<MMID>> {
        it.cb()
      }
    }
    launch {
      doObserve("/observe/install-apps") {
        runningApps.emitChangeBackground(adds, updates, removes)
      }
    }
  }

  private suspend fun IHandlerContext.getRunningApp(ipc: Ipc) = openAppLock.withLock("app") {
    this@DeskNMM.getRunningApp(ipc) ?: throwException(
      HttpStatusCode.NotFound, "microModule(${ipc.remote.mmid}) is not an application"
    )
  }

  private val openAppLock = ReasonLock()
  suspend fun IHandlerContext.openOrActivateAppWindow(
    ipc: Ipc, desktopController: DesktopController
  ): WindowController {
    val appId = ipc.remote.mmid;
    debugDesk("openOrActivateAppWindow", appId)
    try {
      /// desk直接为应用打开窗口，因为窗口由desk统一管理，所以由desk窗口，并提供句柄
      val appMainWindow = getAppMainWindow(ipc)

      /// 将所有的窗口聚焦
      desktopController.desktopWindowsManager.focusWindow(appId)
      return appMainWindow
    } catch (e: Exception) {
      desktopController.showAlert(e)
      e.printStackTrace()
      throwException(cause = e)
    }
  }

  suspend fun IHandlerContext.getAppMainWindow(ipc: Ipc) = openAppLock.withLock("window") {
    val runningApp = getRunningApp(ipc)
    /// desk直接为应用打开窗口，因为窗口由desk统一管理，所以由desk窗口，并提供句柄
    val appMainWindow = runningApp.getMainWindow()
    appMainWindow
  }

  suspend fun IHandlerContext.createModal(ipc: Ipc) =
    openAppLock.withLock("write-modal") {
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

  fun IHandlerContext.getWindow() = request.query("wid").let { wid ->
    windowInstancesManager.get(wid) ?: throw Exception("No Found by window id: '$wid'")
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    listenApps()
    // 创建桌面和任务的服务
    val taskbarServer = this.createTaskbarWebServer()
    val desktopServer = this.createDesktopWebServer()
    val deskSessionId = randomUUID()

    val desktopController = DesktopController(this, desktopServer, runningApps)
    val taskBarController =
      TaskbarController(deskSessionId, this, desktopController, taskbarServer, runningApps)
    val deskControllers = DeskControllers(desktopController, taskBarController, this)
    controllersMap[deskSessionId] = deskControllers

    this.onAfterShutdown {
      runningApps.reset()
      controllersMap.remove(deskSessionId)
    }

    /// 实现协议
    windowProtocol(desktopController)

    /// 内部接口
    routes(
      //
      "/readFile" bind HttpMethod.Get to definePureResponse {
        nativeFetch(request.query("url"))
      },
      // readAccept
      "/readAccept." bindPrefix HttpMethod.Get to definePureResponse {
        return@definePureResponse PureResponse(
          HttpStatusCode.OK,
          body = PureStringBody("""{"accept":"${request.headers.get("Accept")}"}""")
        )
      },
      //
      "/openAppOrActivate" bind HttpMethod.Get to defineStringResponse {
        val mmid = request.query("app_id")
        debugDesk("openAppOrActivate", "requestMMID=$mmid")
        // 内部接口，所以ipc通过connect获得
        openOrActivateAppWindow(connect(mmid, request), desktopController).id
      },
      // 获取isMaximized 的值
      "/toggleMaximize" bind HttpMethod.Get to defineBooleanResponse {
        val mmid = request.query("app_id")
        return@defineBooleanResponse desktopController.desktopWindowsManager.toggleMaximize(mmid)
      },
      // 关闭app
      "/closeApp" bind HttpMethod.Get to defineBooleanResponse {
        val mmid = request.query("app_id")
        when (val runningApp = runningApps[mmid]) {
          null -> false
          else -> {
            runningApp.closeMainWindow();
            true
          }
        }
      },
      // 获取全部app数据
      "/desktop/apps" bind HttpMethod.Get to defineJsonResponse {
        debugDesk("/desktop/apps", desktopController.getDesktopApps())
        return@defineJsonResponse desktopController.getDesktopApps().toJsonElement()
      },
      // 监听所有app数据
      "/desktop/observe/apps" bind HttpMethod.Get to defineJsonLineResponse {
        desktopController.onUpdate {
          try {
            emit(desktopController.getDesktopApps())
          } catch (e: Throwable) {
            end(reason = e)
          }
        }.removeWhen(onDispose)
        desktopController.updateSignal.emit()
      },
      // 获取所有taskbar数据
      "/taskbar/apps" bind HttpMethod.Get to defineJsonResponse {
        val limit = request.queryOrNull("limit")?.toInt() ?: Int.MAX_VALUE
        return@defineJsonResponse taskBarController.getTaskbarAppList(limit).toJsonElement()
      },
      // 监听所有taskbar数据
      "/taskbar/observe/apps" bind HttpMethod.Get to defineJsonLineResponse {
        val limit = request.queryOrNull("limit")?.toInt() ?: Int.MAX_VALUE
        debugDesk("/taskbar/observe/apps", limit)
        taskBarController.onUpdate {
          try {
            emit(taskBarController.getTaskbarAppList(limit))
          } catch (e: Exception) {
            end(reason = e)
          }
        }.removeWhen(onDispose)
        taskBarController.updateSignal.emit()
      },
      // 监听所有taskbar状态
      "/taskbar/observe/status" bind HttpMethod.Get to defineJsonLineResponse {
        debugDesk("/taskbar/observe/status")
        taskBarController.onStatus { status ->
          emit(status)
        }.removeWhen(onDispose)
      },
      // 负责resize taskbar大小
      "/taskbar/resize" bind HttpMethod.Get to defineJsonResponse {
        val size = request.queryAs<TaskbarController.ReSize>()
        debugDesk("/taskbar/resize", "$size")
        taskBarController.resize(size)
        size.toJsonElement()
      },
      // 切换到桌面
      "/taskbar/toggle-desktop-view" bind HttpMethod.Get to defineBooleanResponse {
        taskBarController.toggleDesktopView()
        true
      },
      // 在app为全屏的时候，调出周围的高斯模糊，调整完全的taskbar
      "/taskbar/toggle-float-button-mode" bind HttpMethod.Get to defineBooleanResponse {
        taskBarController.taskbarView.toggleFloatWindow(
          request.queryOrNull("open")?.toBooleanStrictOrNull()
        )
      }).protected(setOf("jmm.browser.dweb", mmid)).cors()

    onActivity {
      startDesktopView(deskSessionId)
    }
    desktopController.onActivity {
      startDesktopView(deskSessionId)
    }
    startDesktopView(deskSessionId)
    /// 等待主视图启动完成
    deskControllers.activityPo.waitPromise()
  }

  private fun startDesktopView(deskSessionId: String) {
    /// 启动对应的Activity视图，如果在后端也需要唤醒到最前面，所以需要在AndroidManifest.xml 配置 launchMode 为 singleTask
    startAppActivity(DesktopActivity::class.java) { intent ->
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
      // 不可以添加 Intent.FLAG_ACTIVITY_NEW_DOCUMENT ，否则 TaskbarActivity 就没发和 DesktopActivity 混合渲染、点击穿透
      intent.putExtras(Bundle().apply {
        putString("deskSessionId", deskSessionId)
      })
    }
  }

  override suspend fun _shutdown() {
  }

  private val API_PREFIX = "/api/"
  private suspend fun createTaskbarWebServer(): HttpDwebServer {
    val taskbarServer =
      createHttpDwebServer(DwebHttpServerOptions(subdomain = "taskbar", port = 433))
    taskbarServer.listen().onRequest { (request, ipc) ->
      val pathName = request.uri.encodedPathAndQuery
      val url = if (pathName.startsWith(API_PREFIX)) {
        val internalUri = pathName.substring(API_PREFIX.length)
        "file://$internalUri"
      } else {
        "file:///sys/browser/desk${pathName}?mode=stream"
      }
      val response = nativeFetch(request.toPure().copy(href = url))
      ipc.postMessage(IpcResponse.fromResponse(request.req_id, response, ipc))
    }
    return taskbarServer
  }

  private suspend fun createDesktopWebServer(): HttpDwebServer {
    val desktopServer =
      createHttpDwebServer(DwebHttpServerOptions(subdomain = "desktop", port = 433))
    desktopServer.listen().onRequest { (request, ipc) ->
      val pathName = request.uri.encodedPathAndQuery
      val url = if (pathName.startsWith(API_PREFIX)) {
        val internalUri = pathName.substring(API_PREFIX.length)
        "file://$internalUri"
      } else {
        "file:///sys/browser/desk${request.uri.encodedPath}?mode=stream"
      }
      val response = nativeFetch(request.toPure().copy(href = url))
      ipc.postMessage(
        IpcResponse.fromResponse(
          request.req_id, PureResponse.build(response) { appendHeaders(CORS_HEADERS) }, ipc
        )
      )
    }
    return desktopServer
  }
}
