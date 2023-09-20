package info.bagen.dwebbrowser.microService.browser.desk

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.jmm.EIpcEvent
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.ChangeState
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.readByteArray
import org.dweb_browser.helper.readInt
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.cors
import org.dweb_browser.microservice.help.types.CommonAppManifest
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.types.MMID
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.dweb_browser.microservice.ipc.helper.ReadableStream
import org.dweb_browser.microservice.sys.dns.nativeFetch
import org.dweb_browser.microservice.sys.http.CORS_HEADERS
import org.dweb_browser.microservice.sys.http.DwebHttpServerOptions
import org.dweb_browser.microservice.sys.http.HttpDwebServer
import org.dweb_browser.microservice.sys.http.createHttpDwebServer
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.composite
import org.http4k.lens.float
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.util.UUID

fun debugDesk(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("desk", tag, msg, err)

class DesktopNMM : NativeMicroModule("desk.browser.dweb", "Desk") {
  init {
    categories = mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Desktop);
  }

  private val runningApps = ChangeableMap<MMID, Ipc>()
  private suspend fun addRunningApp(mmid: MMID) :Ipc?{
    /// 如果成功打开，将它“追加”到列表中
    return when (val ipc = runningApps[mmid]) {
      null -> {
        val ipc = connect(mmid)
        if(ipc.remote.categories.contains(MICRO_MODULE_CATEGORY.Application)){
          runningApps[mmid] = ipc
          /// 如果应用关闭，将它从列表中移除
          ipc.onClose {
            runningApps.remove(mmid)
          }
          ipc
        } else null
      }
      else -> ipc
    }
  }

  companion object {
    data class DeskControllers(
      val desktopController: DesktopController, val taskbarController: TaskbarController
    ){
      val activityPo = PromiseOut<Activity>()
    }

    val controllersMap = mutableMapOf<String, DeskControllers>()
  }

  val queryAppId = Query.string().required("app_id")
  val queryUrl = Query.string().required("url")
  val queryLimit = Query.int().optional("limit")
  val queryResize = Query.composite {
    TaskbarController.ReSize(
      width = float().required("width")(it), height = float().required("height")(it)
    )
  }
  val queryOpen = Query.boolean().optional("open")

  private suspend fun listenApps() = ioAsyncScope.launch {
    val (openedAppIpc) = bootstrapContext.dns.connect("dns.std.dweb")
    suspend fun doObserve(urlPath: String,cb: suspend ChangeState<MMID>.()->Unit) {
      val res = openedAppIpc.request(urlPath)
      val stream = res.body.stream
      var cache = ""
      while (true) {
        val size = stream.available()
        if (size <= 0) {
          break;
        }
        cache += stream.readByteArray(size).toString(Charsets.UTF_8)
        if (!cache.contains("\n")) {
          continue
        }
        while (true) {
          val lines = cache.split("\n", limit = 2)
          if (lines.size > 1) {
            cache = lines[1]
            val line = lines[0]
            val state = Json.decodeFromString<ChangeState<MMID>>(line)
            state.cb()
          } else {
            break
          }
        }
      }
    }
    launch {
      doObserve("/observe/install-apps"){
        runningApps.emitChangeBackground(adds, updates, removes)
      }
    }
    launch {
      doObserve("/observe/running-apps"){
        for (mmid in adds) {
          addRunningApp(mmid)
        }
      }
    }
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    debugDesk("START")
    listenApps()
    // 创建桌面和任务的服务
    val taskbarServer = this.createTaskbarWebServer()
    val desktopServer = this.createDesktopWebServer()
    val deskSessionId = UUID.randomUUID().toString()

    val desktopController =
      DesktopController(deskSessionId, this, desktopServer, runningApps)
    val taskBarController =
      TaskbarController(deskSessionId, this, desktopController, taskbarServer, runningApps)
    val deskControllers = DeskControllers(desktopController, taskBarController)
    controllersMap[deskSessionId] = deskControllers

    this.onAfterShutdown {
      runningApps.reset()
      controllersMap.remove(deskSessionId)
    }

    apiRouting = routes(
      "/readFile" bind Method.GET to defineHandler { request ->
        val url = queryUrl(request)
        return@defineHandler nativeFetch(url)
      },
      // readAccept
      "{accept:readAccept\\.\\w+\$}" bind Method.GET to defineHandler { request ->
        return@defineHandler Response(Status.OK).body("""{"accept":"${request.header("Accept")}"}""")
      },
      "/openAppOrActivate" bind Method.GET to defineHandler { request ->
        val mmid = queryAppId(request)
        debugDesk("/openAppOrActivate", mmid)
        try {
          val ipc = addRunningApp(mmid)
          ipc?.postMessage(IpcEvent.fromUtf8(EIpcEvent.Activity.event, ""))

          /// 将所有的窗口聚焦，这个行为不依赖于 Activity 事件，而是Desk模块自身托管窗口的行为
          desktopController.desktopWindowsManager.focusWindow(mmid)

          return@defineHandler true
        } catch (e: Exception) {
          desktopController.showAlert(e)
          e.printStackTrace()
          return@defineHandler false
        }
      },
      // 获取isMaximized 的值
      "/toggleMaximize" bind Method.GET to defineHandler { request ->
        val mmid = queryAppId(request)
        return@defineHandler desktopController.desktopWindowsManager.toggleMaximize(mmid)
      },
      "/closeApp" bind Method.GET to defineHandler { request ->
        val mmid = queryAppId(request);
        if (runningApps.containsKey(mmid)) {
          return@defineHandler bootstrapContext.dns.close(mmid)
        }
        return@defineHandler false
      },
      "/desktop/apps" bind Method.GET to defineHandler { _ ->
        debugDesk("/desktop/apps", desktopController.getDesktopApps())
        return@defineHandler desktopController.getDesktopApps()
      },
      "/desktop/observe/apps" bind Method.GET to defineHandler { _, ipc ->
        val inputStream = ReadableStream(onStart = { controller ->
          val off = desktopController.onUpdate {
            try {
              val jsonData = Json.encodeToString(desktopController.getDesktopApps())
              controller.enqueue((jsonData + "\n").toByteArray())
            } catch (e: Exception) {
              controller.close()
              e.printStackTrace()
            }
          }
          ipc.onClose {
            off()
            controller.close()
          }
        })
        desktopController.updateSignal.emit()
        return@defineHandler Response(Status.OK).body(inputStream)
      },
      "/taskbar/apps" bind Method.GET to defineHandler { request ->
        val limit = queryLimit(request) ?: Int.MAX_VALUE
        return@defineHandler taskBarController.getTaskbarAppList(limit)
      },
      "/taskbar/observe/apps" bind Method.GET to defineHandler { request, ipc ->
        val limit = queryLimit(request) ?: Int.MAX_VALUE
        debugDesk("/taskbar/observe/apps", limit)
        val inputStream = ReadableStream(onStart = { controller ->
          val off = taskBarController.onUpdate {
            try {
              val jsonData = Json.encodeToString(taskBarController.getTaskbarAppList(limit))
              controller.enqueue((jsonData + "\n").toByteArray())
            } catch (e: Exception) {
              controller.close()
              e.printStackTrace()
            }
          }
          ipc.onClose {
            off()
            controller.close()
          }
        })
        taskBarController.updateSignal.emit()
        return@defineHandler Response(Status.OK).body(inputStream)
      },
      "/taskbar/observe/status" bind Method.GET to defineHandler { _, ipc ->
        debugDesk("/taskbar/observe/status")
        val inputStream = ReadableStream(onStart = { controller ->
          val off = taskBarController.onStatus { status ->
            try {
              val jsonData = Json.encodeToString(status)
              controller.enqueueBackground((jsonData + "\n").toByteArray())
            } catch (e: Exception) {
              controller.close()
              e.printStackTrace()
            }
          }
          ipc.onClose {
            off()
            controller.close()
          }
        })
        return@defineHandler Response(Status.OK).body(inputStream)
      },
      "/taskbar/resize" bind Method.GET to defineJsonResponse { request ->
        val size = queryResize(request)
        debugDesk("/taskbar/resize", "$size")
        taskBarController.resize(size)
        size.toJsonElement()
      },
      // 切换到桌面
      "/taskbar/toggle-desktop-view" bind Method.GET to defineBooleanResponse {
        taskBarController.toggleDesktopView()
        true
      },
      // 在app为全屏的时候，调出周围的高斯模糊，调整完全的taskbar
      "/taskbar/toggle-float-button-mode" bind Method.GET to defineBooleanResponse {
        val open = queryOpen(request)
        taskBarController.taskbarView.toggleFloatWindow(open)
      }
    ).cors()

    onActivity {
      startActivity(deskSessionId)
    }
    desktopController.onActivity {
      startActivity(deskSessionId)
    }
    startActivity(deskSessionId)
    deskControllers.activityPo.waitPromise()
    debugDesk("BBBBBB")
  }

  private fun startActivity(deskSessionId: String) {
    /// 启动对应的Activity视图，如果在后端也需要唤醒到最前面，所以需要在AndroidManifest.xml 配置 launchMode 为 singleTask
    App.startActivity(DesktopActivity::class.java) { intent ->
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
      val pathName = request.uri.path
      val url = if (pathName.startsWith(API_PREFIX)) {
        val internalUri = pathName.substring(API_PREFIX.length)
        "file://$internalUri?${request.uri.query}"
      } else {
        "file:///sys/browser/desk${pathName}?mode=stream"
      }
      val response =
        nativeFetch(Request(request.method.http4kMethod, url).headers(request.headers.toList()))
      ipc.postMessage(IpcResponse.fromResponse(request.req_id, response, ipc))
    }
    return taskbarServer
  }

  private suspend fun createDesktopWebServer(): HttpDwebServer {
    val desktopServer =
      createHttpDwebServer(DwebHttpServerOptions(subdomain = "desktop", port = 433))
    desktopServer.listen().onRequest { (request, ipc) ->
      val pathName = request.uri.path
      val url = if (pathName.startsWith(API_PREFIX)) {
        val internalUri = pathName.substring(API_PREFIX.length)
        "file://$internalUri?${request.uri.query}"
      } else {
        "file:///sys/browser/desk${pathName}?mode=stream"
      }
      val response =
        nativeFetch(Request(request.method.http4kMethod, url).headers(request.headers.toList()))
      ipc.postMessage(IpcResponse.fromResponse(request.req_id, response.headers(CORS_HEADERS), ipc))
    }
    return desktopServer
  }
}
