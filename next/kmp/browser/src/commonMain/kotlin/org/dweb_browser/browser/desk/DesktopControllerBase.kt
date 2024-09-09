package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.desk.types.DeskAppMetaData
import org.dweb_browser.browser.web.WebLinkMicroModule
import org.dweb_browser.browser.web.data.WebLinkManifest
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.helper.OffListener
import org.dweb_browser.helper.SafeHashSet
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.from
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.browser.desk.render.NFSpaceCoordinateLayout

sealed class DesktopControllerBase(
  val viewController: IPureViewController,
  val deskNMM: DeskNMM.DeskRuntime,
) {
  companion object {
    internal suspend fun configSharedRoutes(
      desktopController: DesktopControllerBase,
      deskNMM: DeskNMM.DeskRuntime,
    ) {
      with(deskNMM) {
        deskNMM.routes(
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
        )

        /**
         * 增加一个专门给 web.browser.dweb 调用的 router
         */
        desktopController.loadWebLinks() // 加载存储的数据
        deskNMM.routes(
          /**
           * 添加桌面快捷方式
           */
          "/addWebLink" bind PureMethod.POST by defineBooleanResponse {
            debugDesk("addWebLink", "called")
            val webLinkManifest =
              Json.decodeFromString<WebLinkManifest>(request.body.toPureString())
            debugDesk("addWebLink", "webLinkManifest=$webLinkManifest")
            desktopController.createWebLink(webLinkManifest)
          },
          /**
           * 移除桌面快捷方式
           */
          "/removeWebLink" bind PureMethod.GET by defineBooleanResponse {
            val mmid = request.queryOrNull("app_id") ?: throwException(
              HttpStatusCode.BadRequest,
              "not found app_id"
            )
            debugDesk("removeWebLink", "called => mmid=$mmid")
            desktopController.removeWebLink(mmid)
          },
          /**
           * 打开桌面快捷方式
           */
          "/openBrowser" bind PureMethod.GET by defineBooleanResponse {
            val url = request.queryOrNull("url") ?: throwException(
              HttpStatusCode.BadRequest, "not found url"
            )
            debugDesk("openBrowser", "called => url=$url")
            try {
              nativeFetch(url).boolean()
            } catch (e: Exception) {
              throwException(HttpStatusCode.ExpectationFailed, e.message)
            }
          },
        ).cors()
      }
    }
  }

  internal val appsLayoutStore = DesktopV2AppLayoutStore(deskNMM)

  open suspend fun openAppOrActivate(mmid: MMID) {
    deskNMM.openAppOrActivate(mmid)
  }

  open suspend fun closeApp(mmid: MMID) {
    deskNMM.closeApp(mmid)
  }

  // val newVersionController = NewVersionController(deskNMM, this)
  // 针对 WebLink 的管理部分 begin
  private val webLinkStore = WebLinkStore(deskNMM)
  suspend fun loadWebLinks() {
    webLinkStore.getAll().map { (_, webLinkManifest) ->
      deskNMM.bootstrapContext.dns.install(WebLinkMicroModule(webLinkManifest))
    }
  }

  suspend fun createWebLink(webLinkManifest: WebLinkManifest): Boolean {
    deskNMM.bootstrapContext.dns.query(webLinkManifest.id)?.let { lastWebLink ->
      deskNMM.bootstrapContext.dns.uninstall(lastWebLink.id)
    }
    webLinkStore.set(webLinkManifest.id, webLinkManifest)
    deskNMM.bootstrapContext.dns.install(WebLinkMicroModule(webLinkManifest))
    return true
  }

  private suspend fun removeWebLink(id: MMID): Boolean {
    deskNMM.bootstrapContext.dns.uninstall(id)
    webLinkStore.delete(id)
    return true
  }
  // 针对 WebLink 的管理部分 end


  // 状态更新信号
  internal val updateFlow = MutableSharedFlow<String>()
  val onUpdate = channelFlow {
    val reasons = SafeHashSet<String>()
    updateFlow.onEach {
      reasons.addAll(it.split("|"))
    }.conflate().collect {
      delay(100)
      val result = reasons.sync {
        val result = joinToString("|")
        clear()
        result
      }
      if (result.isNotEmpty()) {
        send(result)
      }
    }
    close()
  }.shareIn(deskNMM.getRuntimeScope(), started = SharingStarted.Eagerly, replay = 1)

  suspend fun detail(mmid: String) {
    deskNMM.nativeFetch("file://jmm.browser.dweb/detail?app_id=$mmid")
  }

  private suspend fun uninstall(mmid: String) {
    deskNMM.nativeFetch("file://jmm.browser.dweb/uninstall?app_id=$mmid")
  }

  suspend fun remove(mmid: MMID, isWebLink: Boolean) {
    appsLayoutStore.removeLayouts(mmid)
    when {
      isWebLink -> removeWebLink(mmid)
      else -> uninstall(mmid)
    }
  }

  suspend fun share(mmid: String) {
    // TODO: 分享
  }

  suspend fun search(words: String) {
    deskNMM.nativeFetch(
      when (words.startsWith("dweb://")) {
        true -> words
        else -> "file://web.browser.dweb/search?q=$words"
      }
    )
  }

  suspend fun getDesktopApps(): List<DeskAppMetaData> {
    val apps =
      deskNMM.bootstrapContext.dns.search(MICRO_MODULE_CATEGORY.Application).toMutableList()
    // 简单的排序再渲染
    val sortList = deskNMM.deskController.appSortList.getApps()
    apps.sortBy { sortList.indexOf(it.mmid) }
    val runApps = apps.map { metaData ->
      return@map DeskAppMetaData().apply {
        running = deskNMM.runningApps.containsKey(metaData.mmid)
        winStates = getDesktopWindowsManager().getWindowStates(metaData.mmid)
        //...复制metaData属性
        assign(metaData.manifest)
      }
    }
    return runApps
  }

  private var preDesktopWindowsManager: DesktopWindowsManager? = null

  private val wmLock = Mutex()

  /**
   * 窗口管理器
   */
  suspend fun getDesktopWindowsManager() = wmLock.withLock {
//    _desktopView.await()
//    val vc = this.activity!!
    DesktopWindowsManager.getOrPutInstance(
      viewController, IPureViewBox.from(viewController)
    ) { dwm ->
      val watchWindows = mutableMapOf<WindowController, OffListener<*>>()
      fun watchAllWindows() {
        watchWindows.keys.subtract(dwm.allWindows).forEach { win ->
          watchWindows.remove(win)?.invoke()
        }
        for (win in dwm.allWindows) {
          if (watchWindows.contains(win)) {
            continue
          }
          watchWindows[win] = win.state.observable.onChange {
            updateFlow.emit(it.key.fieldName)
          }
        }
      }

      /// 但有窗口信号变动的时候，确保 MicroModule.IpcEvent<Activity> 事件被激活
      dwm.allWindowsFlow.collectIn(dwm.viewController.lifecycleScope) {
        watchAllWindows()
        updateFlow.emit("windows")
        _activitySignal.emit()
      }
      watchAllWindows()

      preDesktopWindowsManager?.also { preDwm ->
        deskNMM.scopeLaunch(Dispatchers.Main, cancelable = true) {
          /// 窗口迁移
          preDwm.moveWindowsTo(dwm)
        }
      }
      preDesktopWindowsManager = dwm
    }
  }

  private val _activitySignal = SimpleSignal()
  val onActivity = _activitySignal.toListener()

  @Composable
  abstract fun Render()

  init {
    deskNMM.runningAppsFlow.collectIn(deskNMM.getRuntimeScope()) {
      updateFlow.emit("apps")
    }
  }
}

@Serializable
data class DeskAppLayoutInfo(val screenWidth: Int, val layouts: Map<MMID, NFSpaceCoordinateLayout>)

internal class DesktopV2AppLayoutStore(deskNMM: DeskNMM.DeskRuntime) {

  private val appsLayoutStore = deskNMM.createStore("apps_layout", false)

  suspend fun getStoreAppsLayouts(): List<DeskAppLayoutInfo> {
    return appsLayoutStore.getOrNull("layouts") ?: emptyList()
  }

  suspend fun setStoreAppsLayouts(layouts: List<DeskAppLayoutInfo>) {
    appsLayoutStore.set("layouts", layouts)
  }

  suspend fun removeLayouts(mmid: MMID) {
    val result = getStoreAppsLayouts().map { layoutInfo ->
      layoutInfo.copy(layouts = layoutInfo.layouts.filter { layout ->
        layout.key != mmid
      })
    }
    setStoreAppsLayouts(result)
  }

  suspend fun clearInvaildLayouts(list: List<MMID>) {
    val result = getStoreAppsLayouts().map { layoutInfo ->
      layoutInfo.copy(layouts = layoutInfo.layouts.filter { layout ->
        list.contains(layout.key)
      })
    }
    setStoreAppsLayouts(result)
  }
}