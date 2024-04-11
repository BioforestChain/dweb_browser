package org.dweb_browser.browser.desk

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.router.IHandlerContext
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.bindPrefix
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.createChannel
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
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.invoke
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.helper.withScope
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStringBody
import org.dweb_browser.pure.http.PureTextFrame
import org.dweb_browser.pure.http.queryAs
import org.dweb_browser.pure.http.queryAsOrNull
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.modal.ModalState
import org.dweb_browser.sys.window.core.windowInstancesManager

val debugDesk = Debugger("desk")

class DeskNMM : NativeMicroModule("desk.browser.dweb", "Desk") {
  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Desktop)
    dweb_protocols = listOf("window.sys.dweb")
  }

  companion object {
    data class DeskControllers(
      val desktopController: DesktopController,
      val taskbarController: TaskbarController,
      val deskNMM: DeskNMM.DeskRuntime,
    ) {
      val activityPo = PromiseOut<IPureViewBox>()
    }

    val controllersMap = mutableMapOf<String, DeskControllers>()
  }

  inner class DeskRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {

    private val runningApps = ChangeableMap<MMID, RunningApp>()

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
              runningApps[mmid] = app
              /// 如果应用关闭，将它从列表中移除
              app.onClosed {
                runningApps.remove(mmid)
              }
            }
          } else null
        }

        else -> runningApp
      }
    }


    private suspend fun listenApps() = scopeLaunch(cancelable = true) {
      suspend fun doObserve(urlPath: String, cb: suspend ChangeState<MMID>.() -> Unit) {
        val response = createChannel(urlPath) {
          for (frame in income) {
            when (frame) {
              is PureTextFrame -> {
                Json.decodeFromString<ChangeState<MMID>>(frame.text).also {
                  it.cb()
                }
              }

              else -> {}
            }
          }
        }
        debugDesk("doObserve error", response.status)
      }
      // app排序
      val appSortList = DaskSortStore(this@DeskRuntime)
      launch {
        doObserve("file://dns.std.dweb/observe/install-apps") {
          runningApps.emitChangeBackground(adds, updates, removes)
          // 对排序app列表进行更新
          removes.map {
            appSortList.delete(it)
          }
          adds.map {
            if (!appSortList.has(it)) {
              appSortList.push(it)
            }
          }
        }
      }
    }

    private suspend fun IHandlerContext.getRunningApp(ipc: Ipc) = openAppLock.withLock("app") {
      this@DeskRuntime.getRunningApp(ipc) ?: throwException(
        HttpStatusCode.NotFound, "microModule(${ipc.remote.mmid}) is not an application"
      )
    }

    private val openAppLock = ReasonLock()
    suspend fun IHandlerContext.openOrActivateAppWindow(
      ipc: Ipc, desktopController: DesktopController,
    ): WindowController {
      val appId = ipc.remote.mmid;
      debugDesk("ActivateAppWindow", appId)
      try {
        /// desk直接为应用打开窗口，因为窗口由desk统一管理，所以由desk窗口，并提供句柄
        val appMainWindow = getAppMainWindow(ipc)
        /// 将所有的窗口聚焦
        desktopController.getDesktopWindowsManager().focusWindow(appId)
        return appMainWindow
      } catch (e: Exception) {
        desktopController.showAlert(e)
        e.printStackTrace()
        throwException(cause = e)
      }
    }

    suspend fun IHandlerContext.getAppMainWindow(ipc: Ipc = this.ipc) =
      openAppLock.withLock("window") {
        getWindow {
          val runningApp = getRunningApp(ipc)
          /// desk直接为应用打开窗口，因为窗口由desk统一管理，所以由desk窗口，并提供句柄
          runningApp.getMainWindow()
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
        windowInstancesManager.get(wid) ?: throw Exception("No Found Window by wid: $wid")
      } ?: orElse?.invoke() ?: throw Exception("Fail To Get Window")

    override suspend fun _bootstrap() {
      listenApps()
      // 创建桌面和任务的服务
      val taskbarServer = this.createTaskbarWebServer()
      val desktopServer = this.createDesktopWebServer()
      val deskSessionId = randomUUID()

      val desktopController = DesktopController.create(this, desktopServer, runningApps)
      val taskBarController =
        TaskbarController.create(deskSessionId, this, desktopController, taskbarServer, runningApps)
      val deskControllers = DeskControllers(desktopController, taskBarController, this)
      controllersMap[deskSessionId] = deskControllers

      onShutdown {
        runningApps.reset()
        controllersMap.remove(deskSessionId)
      }

      /// 实现协议
      windowProtocol(desktopController)

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
        //
        "/openAppOrActivate" bind PureMethod.GET by defineBooleanResponse {
          val mmid = request.query("app_id")
          debugDesk("openAppOrActivate", "requestMMID=$mmid")
          // 内部接口，所以ipc通过connect获得
          // 发现desk.js是判断返回值true or false 来显示是否正常启动，所以这边做下修改
          try {
            withScope(mmScope) {
              openOrActivateAppWindow(connect(mmid, request), desktopController).id
            }
            true
          } catch (e: Exception) {
            false
          }
        },
        // 获取isMaximized 的值
        "/toggleMaximize" bind PureMethod.GET by defineBooleanResponse {
          val mmid = request.query("app_id")
          return@defineBooleanResponse desktopController.getDesktopWindowsManager()
            .toggleMaximize(mmid)
        },
        // 关闭app
        "/closeApp" bind PureMethod.GET by defineBooleanResponse {
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
        "/desktop/apps" bind PureMethod.GET by defineJsonResponse {
          debugDesk("/desktop/apps", desktopController.getDesktopApps())
          return@defineJsonResponse desktopController.getDesktopApps().toJsonElement()
        },
        // 监听所有app数据
        "/desktop/observe/apps" byChannel { ctx ->
          val off = desktopController.onUpdate {
//          debugDesk("/desktop/observe/apps", "onUpdate")
            try {
              val apps = desktopController.getDesktopApps()
//            debugDesk("/desktop/observe/apps") { "apps:$apps" }
              ctx.sendJsonLine(apps)
            } catch (e: Throwable) {
              close(cause = e)
            }
          }
          onClose {
            off()
          }
          desktopController.updateSignal.emit()
        },
        // 获取所有taskbar数据
        "/taskbar/apps" bind PureMethod.GET by defineJsonResponse {
          val limit = request.queryOrNull("limit")?.toInt() ?: Int.MAX_VALUE
          return@defineJsonResponse taskBarController.getTaskbarAppList(limit).toJsonElement()
        },
        // 监听所有taskbar数据
        "/taskbar/observe/apps" byChannel { ctx ->
          val limit = request.queryOrNull("limit")?.toInt() ?: Int.MAX_VALUE
          debugDesk("/taskbar/observe/apps", limit)
          val pureChannel = ctx.getChannel()
          taskBarController.onUpdate {
            try {
//            debugDesk("/taskbar/observe/apps") { "onUpdate $pureChannel=>${request.body.toPureString()}" }
              val apps = taskBarController.getTaskbarAppList(limit)
//            debugDesk("/taskbar/observe/apps") { "apps:$apps" }
              ctx.sendJsonLine(apps)
            } catch (e: Exception) {
              close(cause = e)
            }
          }.removeWhen(onClose)
          debugDesk("/taskbar/observe/apps") { "firstEmit $pureChannel=>${request.body.toPureString()}" }
          taskBarController.updateSignal.emit()
        },
        // 监听所有taskbar状态
        "/taskbar/observe/status" byChannel { ctx ->
          debugDesk("deskNMM", "/taskbar/observe/status")
          taskBarController.onStatus { status ->
            ctx.sendJsonLine(status)
          }.removeWhen(onClose)
        },
        // 负责resize taskbar大小
        "/taskbar/resize" bind PureMethod.GET by defineJsonResponse {
          val size = request.queryAs<TaskbarController.ReSize>()
//        debugDesk("get/taskbar/resize", "$size")
          taskBarController.resize(size)
          size.toJsonElement()
        },
        // 切换到桌面
        "/taskbar/toggle-desktop-view" bind PureMethod.GET by defineBooleanResponse {
          taskBarController.toggleDesktopView()
          true
        },
        // 在app为全屏的时候，调出周围的高斯模糊，调整完全的taskbar
        "/taskbar/toggle-float-button-mode" bind PureMethod.GET by defineBooleanResponse {
          taskBarController.toggleFloatWindow(
            request.queryOrNull("open")?.toBooleanStrictOrNull()
          )
        }).cors()

      onActivity {
        startDesktopView(deskSessionId)
      }
      desktopController.onActivity {
        startDesktopView(deskSessionId)
      }
      coroutineScope {
        startDesktopView(deskSessionId)
      }
      /// 等待主视图启动完成
      deskControllers.activityPo.waitPromise()
    }

    override suspend fun _shutdown() {
    }

    private val API_PREFIX = "/api/"
    private suspend fun createTaskbarWebServer(): HttpDwebServer {
      val taskbarServer = createHttpDwebServer(DwebHttpServerOptions(subdomain = "taskbar"))
      val serverIpc = taskbarServer.listen()
      serverIpc.onRequest("TaskbarWebServer").collectIn(mmScope) { event ->
        val ipcServerRequest = event.consume()
        val pathName = ipcServerRequest.uri.encodedPathAndQuery
        val url = if (pathName.startsWith(API_PREFIX)) {
          val internalUri = pathName.substring(API_PREFIX.length)
          "file://$internalUri"
        } else {
          "file:///sys/browser/desk${pathName}?mode=stream"
        }
        val response = nativeFetch(ipcServerRequest.toPure().toClient().copy(href = url))
        serverIpc.postResponse(ipcServerRequest.reqId, response)
      }
      return taskbarServer
    }

    private suspend fun createDesktopWebServer(): HttpDwebServer {
      val desktopServer = createHttpDwebServer(DwebHttpServerOptions(subdomain = "desktop"))
      val serverIpc = desktopServer.listen()
      serverIpc.onRequest("DesktopWebServer").collectIn(mmScope) { event ->
        val ipcServerRequest = event.consume()
        val pathName = ipcServerRequest.uri.encodedPathAndQuery
        val url = if (pathName.startsWith(API_PREFIX)) {
          val internalUri = pathName.substring(API_PREFIX.length)
          "file://$internalUri"
        } else {
          "file:///sys/browser/desk${ipcServerRequest.uri.encodedPath}?mode=stream"
        }
        val response = nativeFetch(ipcServerRequest.toPure().toClient().copy(href = url))
        serverIpc.postResponse(
          ipcServerRequest.reqId,
          PureResponse.build(response) { appendHeaders(CORS_HEADERS) },
        )
      }
      return desktopServer
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = DeskRuntime(bootstrapContext)
}

expect suspend fun DeskNMM.DeskRuntime.startDesktopView(deskSessionId: String)
