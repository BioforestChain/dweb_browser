package info.bagen.dwebbrowser.microService.browser.desk

//import org.dweb_browser.microservice.help.cors
import android.content.Intent
import android.os.Bundle
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.jmm.EIpcEvent
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.ChangeState
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.canRead
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.types.MMID
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.PureStreamBody
import org.dweb_browser.microservice.http.PureStringBody
import org.dweb_browser.microservice.http.bind
import org.dweb_browser.microservice.http.toPure
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.dweb_browser.microservice.ipc.helper.ReadableStream
import org.dweb_browser.microservice.sys.dns.nativeFetch
import org.dweb_browser.microservice.sys.http.CORS_HEADERS
import org.dweb_browser.microservice.sys.http.DwebHttpServerOptions
import org.dweb_browser.microservice.sys.http.HttpDwebServer
import org.dweb_browser.microservice.sys.http.createHttpDwebServer

fun debugDesk(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("desk", tag, msg, err)

class DesktopNMM : NativeMicroModule("desk.browser.dweb", "Desk") {
  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Desktop);
  }

  private val runningApps = ChangeableMap<MMID, Ipc>()
  private suspend fun addRunningApp(mmid: MMID): Ipc? {
    /// 如果成功打开，将它“追加”到列表中
    return when (val ipc = runningApps[mmid]) {
      null -> {
        val ipc = connect(mmid)
        if (ipc.remote.categories.contains(MICRO_MODULE_CATEGORY.Application)) {
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
  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler

  companion object {
    data class DeskControllers(
      val desktopController: DesktopController, val taskbarController: TaskbarController
    ) {
      val activityPo = PromiseOut<Activity>()
    }

    val controllersMap = mutableMapOf<String, DeskControllers>()
  }

//  val queryAppId = Query.string().required("app_id")
//  val queryUrl = Query.string().required("url")
//  val queryLimit = Query.int().optional("limit")
//  val queryResize = Query.composite {
//    TaskbarController.ReSize(
//      width = float().required("width")(it), height = float().required("height")(it)
//    )
//  }
//  val queryOpen = Query.boolean().optional("open")

  private suspend fun listenApps() = ioAsyncScope.launch {
    val (openedAppIpc) = bootstrapContext.dns.connect("dns.std.dweb")
    suspend fun doObserve(urlPath: String, cb: suspend ChangeState<MMID>.() -> Unit) {
      val res = openedAppIpc.request(urlPath)
      val reader = res.stream().getReader("Desk listenApps");
      while (reader.canRead) {
        val state = Json.decodeFromString<ChangeState<MMID>>(reader.readUTF8Line() ?: break)
        state.cb()
      }
    }
    launch {
      doObserve("/observe/install-apps") {
        runningApps.emitChangeBackground(adds, updates, removes)
      }
    }
    launch {
      doObserve("/observe/running-apps") {
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
    val deskSessionId = randomUUID()

    val desktopController = DesktopController(deskSessionId, this, desktopServer, runningApps)
    val taskBarController =
      TaskbarController(deskSessionId, this, desktopController, taskbarServer, runningApps)
    val deskControllers = DeskControllers(desktopController, taskBarController)
    controllersMap[deskSessionId] = deskControllers

    this.onAfterShutdown {
      runningApps.reset()
      controllersMap.remove(deskSessionId)
    }

    routes(
      //
      "/readFile" bind HttpMethod.Get to definePureResponse {
        nativeFetch(request.queryOrFail("url"))
      },
      // readAccept
      "/readAccept." bind HttpMethod.Get to definePureResponse {
        return@definePureResponse PureResponse(
          HttpStatusCode.OK,
          body = PureStringBody("""{"accept":"${request.headers.get("Accept")}"}""")
        )
      },
      //
      "/openAppOrActivate" bind HttpMethod.Get to defineBooleanResponse {
        val mmid = request.queryOrFail("app_id")
        debugDesk("/openAppOrActivate", mmid)
        try {
          val ipc = addRunningApp(mmid)
          ipc?.postMessage(IpcEvent.fromUtf8(EIpcEvent.Activity.event, ""))

          /// 将所有的窗口聚焦，这个行为不依赖于 Activity 事件，而是Desk模块自身托管窗口的行为
          desktopController.desktopWindowsManager.focusWindow(mmid)

          true
        } catch (e: Exception) {
          desktopController.showAlert(e)
          e.printStackTrace()
          false
        }
      },
      // 获取isMaximized 的值
      "/toggleMaximize" bind HttpMethod.Get to defineBooleanResponse {
        val mmid = request.queryOrFail("app_id")
        return@defineBooleanResponse desktopController.desktopWindowsManager.toggleMaximize(mmid)
      },
      //
      "/closeApp" bind HttpMethod.Get to defineBooleanResponse {
        val mmid = request.queryOrFail("app_id")
        if (runningApps.containsKey(mmid)) {
          return@defineBooleanResponse bootstrapContext.dns.close(mmid)
        }
        return@defineBooleanResponse false
      },
      //
      "/desktop/apps" bind HttpMethod.Get to defineJsonResponse() {
        debugDesk("/desktop/apps", desktopController.getDesktopApps())
        return@defineJsonResponse desktopController.getDesktopApps().toJsonElement()
      },
      //
      "/desktop/observe/apps" bind HttpMethod.Get to definePureResponse {
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
          ioAsyncScope.launch {
            controller.awaitClose {
              off()
            }
          }
          ipc.onClose {
            off()
            controller.close()
          }
        })
        ioAsyncScope.launch {
          desktopController.updateSignal.emit()
        }
        return@definePureResponse PureResponse(
          HttpStatusCode.OK, body = PureStreamBody(inputStream.stream)
        )
      },
      //
      "/taskbar/apps" bind HttpMethod.Get to defineJsonResponse {
        val limit = request.query("limit")?.toInt() ?: Int.MAX_VALUE
        return@defineJsonResponse taskBarController.getTaskbarAppList(limit).toJsonElement()
      },
      //
      "/taskbar/observe/apps" bind HttpMethod.Get to definePureResponse {
        val limit = request.query("limit")?.toInt() ?: Int.MAX_VALUE
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
          ioAsyncScope.launch {
            controller.awaitClose {
              off()
            }
          }
          ipc.onClose {
            off()
            controller.close()
          }
        })
        ioAsyncScope.launch {
          taskBarController.updateSignal.emit()
        }
        return@definePureResponse PureResponse(
          HttpStatusCode.OK, body = PureStreamBody(inputStream.stream)
        )
      },
      //
      "/taskbar/observe/status" bind HttpMethod.Get to definePureResponse {
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
          ioAsyncScope.launch {
            controller.awaitClose {
              off()
            }
          }
          ipc.onClose {
            off()
            controller.close()
          }
        })
        return@definePureResponse PureResponse(
          HttpStatusCode.OK, body = PureStreamBody(inputStream.stream)
        )
      },
      //
      "/taskbar/resize" bind HttpMethod.Get to defineJsonResponse {
        val size = request.queryAsObject<TaskbarController.ReSize>()
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
          request.query("open")?.toBooleanStrictOrNull()
        )
      }).cors()

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
          request.req_id, response.appendHeaders(CORS_HEADERS), ipc
        )
      )
    }
    return desktopServer
  }
}
