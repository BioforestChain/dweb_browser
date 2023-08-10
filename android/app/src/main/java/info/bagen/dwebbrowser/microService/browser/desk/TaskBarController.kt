package info.bagen.dwebbrowser.microService.browser.desk

import android.content.res.Resources
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.debounce
import org.dweb_browser.microservice.help.MMID
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.sys.http.HttpDwebServer
import org.http4k.core.query
import java.io.Serializable

class TaskBarController(
  val desktopNMM: DesktopNMM,
  private val taskbarServer: HttpDwebServer,
  private val runningApps: ChangeableMap<MMID, Ipc>
) {
  /** 展示在taskbar中的应用列表 */
  private val _appList = DeskStore.get(DeskStore.TASKBAR_APPS)
  internal fun getFocusApp() = _appList.first()
  internal val updateSignal = SimpleSignal()
  val onUpdate = updateSignal.toListener()
  // 触发状态更新
  internal val stateSignal = Signal<TaskBarState>()
  val onStatus = stateSignal.toListener()

  init {
    /**
     * 绑定 runningApps 集合
     */
    runningApps.onChange { map ->
      /// 将新增的打开应用追加到列表签名
      for (mmid in map.keys) {
        if (!_appList.contains(mmid)) {
          _appList.add(0, mmid) // 追加到第一个
        }
      }
      /// 保存到数据库
      DeskStore.set(DeskStore.TASKBAR_APPS, _appList)
      updateSignal.emit()
    }

    // 监听移除app的改变,可能是增加或者减少
    desktopNMM.bootstrapContext.dns.onChange { map ->
      updateSignal.emit()
    }
  }

  fun getTaskbarAppList(limit: kotlin.Int): List<DeskAppMetaData> {
    val apps = mutableMapOf<MMID, DeskAppMetaData>()
    for (appId in _appList) {
      if (apps.size >= limit) {
        break
      }
      if (appId == desktopNMM.mmid || apps.containsKey(appId)) {
        continue
      }
      val metaData = desktopNMM.bootstrapContext.dns.query(appId)
      if (metaData != null) {
        apps[appId] = DeskAppMetaData(
          //...复制metaData属性
          running = runningApps.contains(appId), winStates = emptyList()
        ).setMetaData(metaData)
      }
    }

    return apps.values.toList()
  }
  // 缓存Resize,用来下次激活taskBarActivity的时候恢复用
  var cacheResize = ReSize(0,0)
  /**
   * 对Taskbar自身进行resize
   * 根据web元素的大小进行自适应调整
   *
   * @returns 如果视图发生了真实的改变（不论是否变成说要的结果），则返回 true
   */
  private suspend fun _resize(reSize: ReSize): ReSize {
    val activity = waitActivityCreated()
    // val metrics = Resources.getSystem().displayMetrics // 当前屏幕密度
    // dp = px / (dpi / 160)
    val width = reSize.width.toDp
    val height = reSize.height.toDp
    cacheResize = ReSize(width,height)
    debugDesk(
      "resize",
      "${reSize.width},${reSize.height} activity"
    )
    // 只能在ui 线程中更新ui
    activity.runOnUiThread {
      activity.window.setLayout(width, height)
    }
    return ReSize(reSize.width, reSize.height)
  }

  suspend fun resize(reSize: ReSize) = debounce(200L) {
    _resize(reSize)
  }

  /**
   * 将其它视图临时最小化到 TaskbarView/TooggleDesktopButton 按钮里头，在此点击该按钮可以释放这些临时视图到原本的状态
   */
  fun toggleDesktopView() {

  }

  private var activityTask = PromiseOut<TaskbarActivity>()
  suspend fun waitActivityCreated() = activityTask.waitPromise()

  var activity: TaskbarActivity? = null
    set(value) {
      if (field == value) {
        return
      }
      field = value
      if (value == null) {
        activityTask = PromiseOut()
      } else {
        activityTask.resolve(value)
      }
    }

  fun getTaskbarUrl() = taskbarServer.startResult.urlInfo.buildInternalUrl().let {
    it.path("/taskbar.html")
      .query("api-base", taskbarServer.startResult.urlInfo.buildPublicUrl().toString())
  }

  data class ReSize(val width: Int, val height: Int)
  data class TaskBarState(val focus: Boolean, val appId: String) : Serializable

  val kotlin.Int.toDp: kotlin.Int
    get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()
}
