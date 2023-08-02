package info.bagen.dwebbrowser.microService.browser.desk

import info.bagen.dwebbrowser.App
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.microservice.help.MMID
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.sys.http.HttpDwebServer
import org.http4k.core.query

class TaskBarController(
  private val desktopNMM: DesktopNMM,
  private val taskbarServer: HttpDwebServer,
  private val runningApps: ChangeableMap<MMID, Ipc>
) {

  /** 展示在taskbar中的应用列表 */
  private val _appList = listOf<MMID>()
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

  /**
   * 对Taskbar自身进行resize
   * 根据web元素的大小进行自适应调整
   *
   * @returns 如果视图发生了真实的改变（不论是否变成说要的结果），则返回 true
   */
  fun resize(width: Number, height: Number) {

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
  fun createMainDwebView() = DWebView(
    activity ?: App.appContext, desktopNMM,
    DWebView.Options(
      url = "",
      onDetachedFromWindowStrategy = DWebView.Options.DetachedFromWindowStrategy.Ignore,
    )
  )


  fun getDesktopUrl() = taskbarServer.startResult.urlInfo.buildInternalUrl().let {
    it.path("/taskbar.html")
      .query("api-base", taskbarServer.startResult.urlInfo.buildPublicUrl().toString())
  }
}