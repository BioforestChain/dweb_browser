package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import io.ktor.http.Url
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.Serializable
import org.dweb_browser.browser.desk.render.ITaskbarV1View
import org.dweb_browser.browser.desk.render.create
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.build
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.queryAs
import org.dweb_browser.pure.http.queryAsOrNull

class TaskbarV1Controller private constructor(
  deskNMM: DeskNMM.DeskRuntime,
  val deskSessionId: String,
  desktopController: DesktopControllerBase,
  private val taskbarServer: HttpDwebServer,
) : TaskbarControllerBase(deskNMM, desktopController) {

  companion object {
    /**
     * 配置给webview的接口
     */
    suspend fun configRoutes(taskBarController: TaskbarV1Controller, deskNMM: DeskNMM.DeskRuntime) {
      with(deskNMM) {
        val mmScope = deskNMM.getRuntimeScope()
        deskNMM.routes(
          // 获取所有taskbar数据
          "/taskbar/apps" bind PureMethod.GET by defineJsonResponse {
            val limit = request.queryOrNull("limit")?.toInt() ?: Int.MAX_VALUE
            return@defineJsonResponse taskBarController.getTaskbarAppList(limit).toJsonElement()
          },
          // 监听所有taskbar数据
          "/taskbar/observe/apps" byChannel { ctx ->
            val limit = request.queryOrNull("limit")?.toInt() ?: Int.MAX_VALUE
            debugDesk(tag = "/taskbar/observe/apps", "limit=$limit")
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
            val job = taskBarController.onStatus.collectIn(mmScope) { status ->
              ctx.sendJsonLine(status)
            }
            onClose {
              job.cancel()
            }
          },
          // 负责resize taskbar大小
          "/taskbar/resize" bind PureMethod.GET by defineJsonResponse {
            val size = request.queryAs<TaskbarV1Controller.ReSize>()
//        debugDesk("get/taskbar/resize", "$size")
            taskBarController.resize(size)
            size.toJsonElement()
          },
          // 切换到桌面
          "/taskbar/toggle-desktop-view" bind PureMethod.GET by defineBooleanResponse {
            taskBarController.toggleDesktopView().join()
            true
          },
          // 在app为全屏的时候，调出周围的高斯模糊，调整完全的taskbar
          "/taskbar/toggle-float-button-mode" bind PureMethod.GET by defineBooleanResponse {
            taskBarController.toggleFloatWindow(
              request.queryOrNull("open")?.toBooleanStrictOrNull()
            ).await()
          },
          "/taskbar/dragging" bind PureMethod.GET by defineBooleanResponse {
            taskBarController.toggleDragging(request.queryAs("dragging"))
          },
        )
      }
    }

    suspend fun create(
      deskSessionId: String,
      deskNMM: DeskNMM.DeskRuntime,
      desktopController: DesktopControllerBase,
    ): TaskbarV1Controller {
      val taskbarServer = DesktopV1Controller.commonWebServerFactory("taskbar", deskNMM)
      val controller = TaskbarV1Controller(deskNMM, deskSessionId, desktopController, taskbarServer)
      configRoutes(controller, deskNMM)
      // 构建webview
      val webview = IDWebView.create(controller.deskNMM, controller.getTaskbarDWebViewOptions())
      controller.taskbarView = ITaskbarV1View.create(controller, webview).also { taskbarView ->
        deskNMM.onBeforeShutdown {
          deskNMM.scopeLaunch(cancelable = false) {
            taskbarView.taskbarDWebView.destroy()
          }
        }
      }
      return controller
    }
  }

  internal lateinit var taskbarView: ITaskbarV1View

  @Composable
  fun TaskbarView(content: @Composable ITaskbarV1View.() -> Unit) {
    taskbarView.content()
  }

  /**
   * 对Taskbar自身进行resize
   * 根据web元素的大小进行自适应调整
   *
   * @returns 如果视图发生了真实的改变（不论是否变成说要的结果），则返回 true
   */

  val desktopIsComposeStyle = envSwitch.isEnabled(ENV_SWITCH_KEY.DESKTOP_STYLE_COMPOSE)
  fun resize(reSize: ReSize) {
    //TODO: 临时处理, 用于防止被compose版本的taskbar被web版本的taskbar影响到。后期确定使用compose版本需要再统一删除掉。
    if (desktopIsComposeStyle) {
      return
    }
    state.layoutWidth = reSize.width
    state.layoutHeight = reSize.height
  }

  fun toggleDragging(dragging: Boolean): Boolean {
    state.dragging = dragging
    return dragging
  }


  fun getTaskbarUrl() = when (val url = envSwitch.get(ENV_SWITCH_KEY.TASKBAR_DEV_URL)) {
    "" -> taskbarServer.startResult.urlInfo.buildInternalUrl().build {
      resolvePath("/taskbar.html")
    }

    else -> Url(url)
  }


  fun getTaskbarDWebViewOptions() = DWebViewOptions(
    url = getTaskbarUrl().toString(),
    openDevTools = envSwitch.isEnabled(ENV_SWITCH_KEY.TASKBAR_DEVTOOLS),
    privateNet = true,
    detachedStrategy = DWebViewOptions.DetachedStrategy.Ignore,
    viewId = 2,
  )

  @Serializable
  data class ReSize(val width: Float, val height: Float)

  @Serializable
  data class TaskBarState(val focus: Boolean, val appId: String)

  @Composable
  override fun Render() {
    taskbarView.Render()
  }
}
