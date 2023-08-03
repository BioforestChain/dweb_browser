package info.bagen.dwebbrowser.microService.browser.desk

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.toMutableStateMap
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.jmm.EIpcEvent
import info.bagen.dwebbrowser.microService.core.AndroidNativeMicroModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.printdebugln
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.MMID
import org.dweb_browser.microservice.help.cors
import org.dweb_browser.microservice.help.gson
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.dweb_browser.microservice.ipc.helper.IpcHeaders
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
import org.http4k.core.then
import org.http4k.filter.AllowAll
import org.http4k.filter.CorsPolicy
import org.http4k.filter.OriginPolicy
import org.http4k.filter.ServerFilters
import org.http4k.lens.Query
import org.http4k.lens.composite
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.util.UUID

fun debugDesktop(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("Desktop", tag, msg, err)

class DesktopNMM : AndroidNativeMicroModule("desk.browser.dweb", "Desk") {
  override val categories =
    mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Desktop);

  private val runningApps = ChangeableMap<MMID, Ipc>()

  companion object {
    val deskControllers = mutableMapOf<String, DeskController>()
    val taskBarControllers = mutableMapOf<String, TaskBarController>()
  }

  val queryAppId = Query.string().required("app_id")
  val queryUrl = Query.string().required("url")
  val queryLimit = Query.int().optional("limit")
  val queryResize = Query.composite {
    ReSize(
      width = int().required("width")(it), height = int().required("height")(it)
    )
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val taskbarServer = this.createTaskbarWebServer()
    val desktopServer = this.createDesktopWebServer()
    val deskController = DeskController(this, desktopServer, runningApps)
    val deskSessionId = UUID.randomUUID().toString()
    deskControllers[deskSessionId] = deskController
    val taskBarController = TaskBarController(this, taskbarServer, runningApps)
    val taskBarSessionId = UUID.randomUUID().toString()
    taskBarControllers[taskBarSessionId] = taskBarController

    this.onAfterShutdown {
      runningApps.reset()
      deskControllers.remove(deskSessionId)
      taskBarControllers.remove(taskBarSessionId)
    }

    apiRouting =
      ServerFilters.Cors(CorsPolicy(OriginPolicy.AllowAll(), listOf("*"), Method.values().toList()))
        .then(
          routes(
            "/readFile" bind Method.GET to defineHandler { request ->
              val url = queryUrl(request)
              return@defineHandler nativeFetch(url)
            },
            // readAccept
            "{accept:readAccept\\.\\w+\$}" bind Method.GET to defineHandler { request ->
              return@defineHandler Response(Status.OK).body("""{"accept":"${request.header("Accept")}"}""")
            },
            "/openAppOrActivate" bind Method.GET to defineHandler { request ->
              println("openAppOrActivate!!")
              val mmid = queryAppId(request)
              try {
                val ipc = runningApps[mmid] ?: connect(mmid)
                ipc.postMessage(IpcEvent.fromUtf8(EIpcEvent.Activity.event, ""))
                /// 如果成功打开，将它“追加”到列表中
                runningApps[mmid] = ipc
                /// 如果应用关闭，将它从列表中移除
                ipc.onClose {
                  runningApps.remove(mmid)
                }

                /// 将所有的窗口聚焦，这个行为不依赖于 Activity 事件，而是Desk模块自身托管窗口的行为
                deskController.desktopWindowsManager.focus(mmid)

                return@defineHandler true
              } catch (e: Exception) {
                return@defineHandler false
              }
            },
            "/toggleMaximize" bind Method.GET to defineHandler { request ->
              val mmid = queryAppId(request)
              return@defineHandler deskController.desktopWindowsManager.toggleMaximize(mmid)
            },
            "/closeApp" bind Method.GET to defineHandler { request ->
              val mmid = queryAppId(request);
              var closed = false;
              if (runningApps.containsKey(mmid)) {
                closed = bootstrapContext.dns.close(mmid);
                if (closed) {
                  runningApps.remove(mmid)
                }
              }
              return@defineHandler closed
            },
            "/desktop/apps" bind Method.GET to defineHandler { _ ->
              debugDesktop("/desktop/apps", deskController.getDesktopApps())
              return@defineHandler deskController.getDesktopApps()
            },
            "/desktop/observe/apps" bind Method.GET to defineHandler { _, ipc ->
              val inputStream = ReadableStream(onStart = { controller ->
                val off = taskBarController.onUpdate {
                  try {
                    withContext(Dispatchers.IO) {
                      controller.enqueue((gson.toJson(deskController.getDesktopApps()) + "\n").toByteArray())
                    }
                  } catch (e: Exception) {
                    controller.close()
                    e.printStackTrace()
                  }
                }
                ipc.onClose {
                  off(Unit)
                  controller.close()
                }
              })
              taskBarController.updateSignal.emit()
              return@defineHandler Response(Status.OK).body(inputStream)
            },
            "/taskbar/apps" bind Method.GET to defineHandler { request ->
              val limit = queryLimit(request) ?: Int.MAX_VALUE
              return@defineHandler taskBarController.getTaskbarAppList(limit)
            },
            "/taskbar/observe/apps" bind Method.GET to defineHandler { request, ipc ->
              val limit = queryLimit(request) ?: Int.MAX_VALUE
              debugDesktop("/taskbar/observe/apps", taskBarController.getTaskbarAppList(limit))
              val inputStream = ReadableStream(onStart = { controller ->
                val off = taskBarController.onUpdate {
                  try {
                    withContext(Dispatchers.IO) {
                      controller.enqueue((gson.toJson(taskBarController.getTaskbarAppList(limit)) + "\n").toByteArray())
                    }
                  } catch (e: Exception) {
                    controller.close()
                    e.printStackTrace()
                  }
                }
                ipc.onClose {
                  off(Unit)
                  controller.close()
                }
              })
              taskBarController.updateSignal.emit()
              return@defineHandler Response(Status.OK).body(inputStream)
            },
            "/taskbar/resize" bind Method.GET to defineHandler { request ->
              val size = queryResize(request)
              taskBarController.resize(size.width, size.height)
              return@defineHandler Response(Status.OK)
            },
            "/taskbar/toggle-desktop-view" bind Method.GET to defineHandler { request ->
              taskBarController.toggleDesktopView()
              return@defineHandler Response(Status.OK)
            },
          ).cors()
        )


    /// 启动对应的Activity视图
    App.startActivity(DesktopActivity::class.java) { intent ->
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
      // 不可以添加 Intent.FLAG_ACTIVITY_NEW_DOCUMENT ，否则 TaskbarActivity 就没发和 DesktopActivity 混合渲染、点击穿透
      intent.putExtras(Bundle().apply {
        putString("deskSessionId", deskSessionId)
        putString("taskBarSessionId", taskBarSessionId)
      })
    }
    val activity = deskController.waitActivityCreated()

    onActivity {
      // TODO 如果收到激活指令，应该将activity唤醒到最前端
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
        val internalUri = request.uri.path(request.uri.path.substring(API_PREFIX.length))
        val search = ""
        "file://$internalUri$search"
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
        val internalUri = request.uri.path(request.uri.path.substring(API_PREFIX.length))
        val search = ""
        "file://$internalUri$search"
      } else {
        "file:///sys/browser/desk${pathName}?mode=stream"
      }
      val response =
        nativeFetch(Request(request.method.http4kMethod, url).headers(request.headers.toList()))
      ipc.postMessage(IpcResponse.fromResponse(request.req_id, response.headers(CORS_HEADERS), ipc))
    }
    return desktopServer
  }

  data class ReSize(val width: Number, val height: Number)
}
