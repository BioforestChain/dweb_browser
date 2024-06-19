package org.dweb_browser.browser.desk

import dweb_browser_kmp.browser.generated.resources.Res
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.web.debugBrowser
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.router.IHandlerContext
import org.dweb_browser.core.http.router.ResponseException
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.bindPrefix
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.helper.IpcServerRequest
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.channelRequest
import org.dweb_browser.core.std.dns.ext.onActivity
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.ext.ResponseLocalFileBase
import org.dweb_browser.core.std.http.CORS_HEADERS
import org.dweb_browser.core.std.http.DwebHttpServerOptions
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.core.std.http.createHttpDwebServer
import org.dweb_browser.helper.ChangeState
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.Producer
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.ReasonLock
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStringBody
import org.dweb_browser.pure.http.PureTextFrame
import org.dweb_browser.pure.http.initCors
import org.dweb_browser.pure.http.queryAs
import org.dweb_browser.pure.http.queryAsOrNull
import org.dweb_browser.sys.toast.ext.showToast
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.modal.ModalState
import org.dweb_browser.sys.window.core.windowInstancesManager
import org.jetbrains.compose.resources.ExperimentalResourceApi

val debugDesk = Debugger("desk")

class DeskNMM : NativeMicroModule("desk.browser.dweb", "Desk") {
  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Desktop)
    dweb_protocols = listOf("window.sys.dweb", "window.std.dweb")
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

  @OptIn(ExperimentalResourceApi::class)
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
              app.ipc.onClosed {
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
        val response = channelRequest(urlPath) {
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
      val appId = ipc.remote.mmid
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

    override suspend fun _bootstrap() {
      listenApps()
      // 创建桌面和任务的服务
      val taskbarServer = this.createTaskbarWebServer()
      val desktopServer = this.createDesktopWebServer()
      val deskSessionId = randomUUID()

      val desktopController = DesktopController.create(this, desktopServer, runningApps)
      val taskBarController = TaskbarController.create(deskSessionId, this, desktopController, taskbarServer, runningApps)
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
        "/openAppOrActivate" bind PureMethod.GET by defineEmptyResponse {
          val mmid = request.query("app_id")
          debugDesk("openAppOrActivate", "requestMMID=$mmid")
          // 内部接口，所以ipc通过connect获得
          val targetIpc = connect(mmid, request)
          debugDesk("openAppOrActivate", "targetIpc=$targetIpc")
          // 发现desk.js是判断返回值true or false 来显示是否正常启动，所以这边做下修改
          openOrActivateAppWindow(targetIpc, desktopController).id
        },
        // 获取isMaximized 的值
        "/toggleMaximize" bind PureMethod.GET by defineBooleanResponse {
          val mmid = request.query("app_id")
          return@defineBooleanResponse desktopController.getDesktopWindowsManager()
            .toggleMaximize(mmid)
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
        // 获取全部app数据
        "/desktop/apps" bind PureMethod.GET by defineJsonResponse {
          debugDesk("/desktop/apps", desktopController.getDesktopApps())
          return@defineJsonResponse desktopController.getDesktopApps().toJsonElement()
        },
        // 监听所有app数据
        "/desktop/observe/apps" byChannel { ctx ->
          // 默认不同步 bounds 字段，否则move的时候数据量会非常大
          val enableBounds = request.queryAsOrNull<Boolean>("bounds") ?: false
          val job = desktopController.onUpdate.run {
            when {
              enableBounds -> this
              // 如果只有 bounds ，那么忽略，不发送
              else -> filter { it != "bounds" }
            }
          }.collectIn(mmScope) {
            debugDesk("/desktop/observe/apps") { "changes=$it" }
            try {
              val apps = desktopController.getDesktopApps()
              ctx.sendJsonLine(apps)
            } catch (e: Throwable) {
              close(cause = e)
            }
          }
          onClose {
            job.cancel()
          }
          desktopController.updateFlow.emit("init")
        },
        // 获取所有taskbar数据
        "/taskbar/apps" bind PureMethod.GET by defineJsonResponse {
          val limit = request.queryOrNull("limit")?.toInt() ?: Int.MAX_VALUE
          return@defineJsonResponse taskBarController.getTaskbarAppList(limit).toJsonElement()
        },
        // 监听所有taskbar数据
        "/taskbar/observe/apps" byChannel { ctx ->
          val limit = request.queryOrNull("limit")?.toInt() ?: Int.MAX_VALUE
          debugDesk("/taskbar/observe/apps", "limit=$limit")
          // 默认不同步 bounds 字段，否则move的时候数据量会非常大
          val enableBounds = request.queryAsOrNull<Boolean>("bounds") ?: false
          val job = taskBarController.onUpdate.run {
            when {
              enableBounds -> this
              // 如果只有 bounds ，那么忽略，不发送
              else -> filter { it != "bounds" }
            }
          }.collectIn(mmScope) {
            debugDesk("/taskbar/observe/apps") { "changes=$it" }
            try {
              val apps = taskBarController.getTaskbarAppList(limit)
              ctx.sendJsonLine(apps)
            } catch (e: Exception) {
              close(cause = e)
            }
          }
          onClose {
            job.cancel()
          }
          debugDesk("/taskbar/observe/apps") { "firstEmit =>${request.body.toPureString()}" }
          taskBarController.updateFlow.emit("init")
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
        },
        "/taskbar/dragging" bind PureMethod.GET by defineBooleanResponse {
          taskBarController.toggleDragging(request.queryAs("dragging"))
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

      onActivity {
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
      serverIpc.onRequest("TaskbarWebServer").collectIn(mmScope, commonWebServerFactory(serverIpc))
      return taskbarServer
    }

    private suspend fun createDesktopWebServer(): HttpDwebServer {
      val desktopServer = createHttpDwebServer(DwebHttpServerOptions(subdomain = "desktop"))
      val serverIpc = desktopServer.listen()
      serverIpc.onRequest("DesktopWebServer").collectIn(mmScope, commonWebServerFactory(serverIpc))
      return desktopServer
    }

    private fun commonWebServerFactory(serverIpc: Ipc) =
      FlowCollector<Producer<IpcServerRequest>.Event> { event ->
        val ipcServerRequest = event.consume()
        val pathName = ipcServerRequest.uri.encodedPathAndQuery
        val pureResponse = if (pathName.startsWith(API_PREFIX)) {
          val apiUri = "file://${pathName.substring(API_PREFIX.length)}"
          val response = nativeFetch(ipcServerRequest.toPure().toClient().copy(href = apiUri))
          PureResponse.build(response) { appendHeaders(CORS_HEADERS) }
        } else {
          val filePath = ipcServerRequest.uri.encodedPath
          val resBinary = Res.readBytes("files/browser-desk${filePath}")
          ResponseLocalFileBase(filePath, false).returnFile(resBinary)
        }
        serverIpc.postResponse(ipcServerRequest.reqId, pureResponse)
      }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = DeskRuntime(bootstrapContext)
}

expect suspend fun DeskNMM.DeskRuntime.startDesktopView(deskSessionId: String)
