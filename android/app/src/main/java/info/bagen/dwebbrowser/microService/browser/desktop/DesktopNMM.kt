package info.bagen.dwebbrowser.microService.browser.desktop

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.toMutableStateMap
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.jmm.EIpcEvent
import info.bagen.dwebbrowser.microService.browser.jmm.JsMicroModule
import info.bagen.dwebbrowser.microService.core.AndroidNativeMicroModule
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dweb_browser.browserUI.database.JsMicroModuleStore
import org.dweb_browser.browserUI.download.compareAppVersionHigh
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.encodeURI
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printdebugln
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.MMID
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
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.composite
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.io.InputStream

fun debugDesktop(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("Desktop", tag, msg, err)

class DesktopNMM : AndroidNativeMicroModule("desk.browser.dweb", "Desk") {
  override val categories =
    mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Desktop);
  private var controller: DesktopController = DesktopController(this)

  companion object {
    private val controllerList = mutableListOf<DesktopController>()
    val desktopController get() = controllerList.firstOrNull()
  }

  private val taskbarAppList = mutableListOf<MMID>()
  private val runningAppsIpc = ChangeableMap<MMID, Ipc>()

  init {
    controllerList.add(DesktopController(this))
    // 监听runningApps的变化
    runningAppsIpc.onChange { map ->
      for (appId in map.keys) {
        // 每次变化对侧边栏图标进行排序(移动到最前面)
        val cur = taskbarAppList.firstOrNull { it == appId }
        cur?.let {
          taskbarAppList.remove(it)
          taskbarAppList.add(it)
        }
      }
    }
  }

  fun getDesktopApps(): List<DeskAppMetaData> {
    var runApps = listOf<DeskAppMetaData>()
    runBlockingCatching(ioAsyncExceptionHandler) {
      val apps = bootstrapContext.dns.search(MICRO_MODULE_CATEGORY.Application)
      runApps = apps.map { metaData ->
        return@map DeskAppMetaData(
          running = runningAppsIpc.containsKey(metaData.mmid),
          isExpand = false
        ).setMetaData(metaData)
      }
    }.getOrThrow()
    return runApps
  }

  /** 展示在taskbar中的应用列表 */
  private val _appList = mutableSetOf<MMID>()

  private fun getTaskbarAppList(limit: Int): List<DeskAppMetaData> {
    val apps = mutableMapOf<MMID, DeskAppMetaData>()
    for (appId in _appList) {
      if (apps.size >= limit) {
        break
      }
      if (appId == mmid || apps.containsKey(appId)) {
        continue
      }
      val metaData = bootstrapContext.dns.query(appId)
      if (metaData != null) {
        apps[appId] = DeskAppMetaData(
          //...复制metaData属性
          running = runningAppsIpc.contains(appId),
        ).setMetaData(metaData)
      }
    }

    return apps.values.toList()

  }
  val queryAppId = Query.string().required("app_id")
  val queryUrl = Query.string().required("url")
  val queryLimit = Query.int().optional("limit")
  val queryResize = Query.composite {
    ReSize(
      width = int().required("width")(it),
      height = int().required("height")(it)
    )
  }
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    this.onAfterShutdown {
      this.runningAppsIpc.reset()
    }
    val taskbarServer = this.createTaskbarWebServer(this.bootstrapContext)
    val desktopServer = this.createDesktopWebServer()

    loadAppInfo()


    apiRouting = routes(
      "/readFile" bind Method.GET to defineHandler { request ->
        val url = queryUrl(request)
        return@defineHandler nativeFetch(url)
      },
      "/openAppOrActivate" bind Method.GET to defineHandler { request ->
        val mmid = queryAppId(request)
        val ipc = runningAppsIpc[mmid] ?: bootstrapContext.dns.connect(mmid).ipcForFromMM
        ipc.postMessage(IpcEvent.fromUtf8(EIpcEvent.Activity.event, ""))
        /// 如果成功打开，将它“追加”到列表中
        runningAppsIpc[mmid] = ipc
        /// 如果应用关闭，将它从列表中移除
        ipc.onClose {
          runningAppsIpc.remove(mmid)
        }
        return@defineHandler true
      },
      "/closeApp" bind Method.GET to defineHandler { request ->
        val mmid = queryAppId(request);
        var closed = false;
        if (runningAppsIpc.containsKey(mmid)) {
          closed = bootstrapContext.dns.close(mmid);
          if (closed) {
            runningAppsIpc.remove(mmid)
          }
        }
        return@defineHandler closed
      },
      "/desktop/apps" bind Method.GET to defineHandler { _ ->
        debugDesktop("/desktop/apps", getDesktopApps())
        return@defineHandler getDesktopApps()
      },
      "/desktop/observe/apps" bind Method.GET to defineHandler { _, ipc ->
        val inputStream = ReadableStream(onStart = { controller ->
          val off = runningAppsIpc.onChange {
            try {
              withContext(Dispatchers.IO) {
                controller.enqueue((gson.toJson(getDesktopApps()) + "\n").toByteArray())
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
        runningAppsIpc.emitChange()
        return@defineHandler Response(Status.OK).body(inputStream)
      },
      "/taskbar/apps" bind Method.GET to defineHandler { request ->
        val limit = queryLimit(request) ?: Int.MAX_VALUE
        return@defineHandler getTaskbarAppList(limit)
      },
      "/taskbar/observe/apps" bind Method.GET to defineHandler {  request, ipc ->
        val limit = queryLimit(request) ?: Int.MAX_VALUE
        val inputStream = ReadableStream(onStart = { controller ->
          val off = runningAppsIpc.onChange {
            try {
              withContext(Dispatchers.IO) {
                controller.enqueue((gson.toJson(getTaskbarAppList(limit)) + "\n").toByteArray())
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
        runningAppsIpc.emitChange()
        return@defineHandler Response(Status.OK).body(inputStream)
      },
      "/taskbar/resize" bind Method.GET to defineHandler { request ->
        val size = queryResize(request)
        return@defineHandler controller.resize(size.width,size.height)
      },
      "/taskbar/toggle-desktop-view" bind Method.GET to defineHandler { request ->
        return@defineHandler controller.toggleDesktopView()
      },
    )
  }
  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }

  /**
   * 从内存中加载数据
   */
  @OptIn(DelicateCoroutinesApi::class)
  private fun loadAppInfo() {
    GlobalScope.launch(ioAsyncExceptionHandler) {
      JsMicroModuleStore.queryAppInfoList().collectLatest { list -> // TODO 只要datastore更新，这边就会实时更新
        debugDesktop("AppInfoDataStore", "size=${list.size}")
        list.map { jsMetaData ->
          // 检测版本
          val lastAppMetaData = bootstrapContext.dns.query(jsMetaData.id)
          lastAppMetaData?.let {
            if (compareAppVersionHigh(it.version, jsMetaData.version)) {
              bootstrapContext.dns.close(it.mmid)
            }
          }
          bootstrapContext.dns.install(JsMicroModule(jsMetaData))
        }
      }
    }
  }

  override suspend fun onActivity(event: IpcEvent, ipc: Ipc) {
    App.startActivity(DesktopActivity::class.java) { intent ->
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
      // 不可以添加 Intent.FLAG_ACTIVITY_NEW_DOCUMENT ，否则 TaskbarActivity 就没发和 DesktopActivity 混合渲染、点击穿透
      intent.putExtras(Bundle().also { b -> b.putString("mmid", mmid) })
    }
    val activity = controller.waitActivityCreated()
    activitySignal.emit(activity)
  }

  private val API_PREFIX = "/api/"

  private suspend fun createTaskbarWebServer(context: BootstrapContext): HttpDwebServer {
    val taskbarServer =
      createHttpDwebServer(DwebHttpServerOptions(subdomain = "taskbar", port = 433))
    taskbarServer.listen().onRequest { (request, ipc) ->
      val pathName = request.uri.path
      val message = if (pathName.startsWith(API_PREFIX)) {
        val internalUri = request.uri.path(request.uri.path.substring(API_PREFIX.length))
        val search = ""
        if (internalUri.host != mmid && context.dns.query(internalUri.host) == null) {
          IpcResponse.fromText(
            request.req_id,
            404,
            IpcHeaders(CORS_HEADERS.toMutableStateMap()),
            "// no found ${internalUri.path}",
            ipc
          )
        }
        IpcResponse.fromResponse(
          request.req_id,
          nativeFetch("file://$internalUri$search"),
          ipc
        )
      } else {
        IpcResponse.fromResponse(
          request.req_id,
          nativeFetch("file:///sys/browser/desk${pathName}?mode=stream"),
          ipc
        )
      }
      ipc.postMessage(message)
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
      val response = nativeFetch(url)
      ipc.postMessage(IpcResponse.fromResponse(request.req_id, response, ipc))
    }
    return desktopServer
  }
  data class ReSize(val width:Number,val height:Number)
}