package org.dweb_browser.browser.desk

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.LocalPureViewController
import org.dweb_browser.helper.platform.bindPureViewController
import org.dweb_browser.helper.platform.from
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme
import org.dweb_browser.helper.platform.unbindPureViewController

class TabletopViewControllerCore(val viewController: IPureViewController) {
  private var tabletopController: TabletopController? = null
  private suspend fun bindController(sessionId: String?): DeskNMM.Companion.DeskControllers {
    /// 解除上一个 controller的activity绑定
    tabletopController?.activity = null

    return DeskNMM.controllersMap[sessionId]?.also { controllers ->
      controllers.tabletopController.activity = viewController
      this.tabletopController = controllers.tabletopController
      controllers.activityPo.resolve(IPureViewBox.from(viewController))
    } ?: throw Exception("no found controller by sessionId: $sessionId")
  }

  private val resumeState = mutableStateOf(false) // 增加字段，为了恢复 taskbarFloatView

  init {
    viewController.onCreate { params ->
      val (desktopController, taskbarController, microModule) = bindController(params.getString("deskSessionId"))
      viewController.addContent {
        val pureViewController = LocalPureViewController.current
        DisposableEffect(pureViewController) {
          microModule.bindPureViewController(pureViewController, true)
          onDispose {
            microModule.unbindPureViewController()
          }
        }

        DwebBrowserAppTheme {
          LaunchedEffect(resumeState) {
            snapshotFlow { resumeState.value }.collect {
              // 增加字段，为了恢复 taskbarFloatView
              if (it) taskbarController.toggleFloatWindow(false)
            }
          }
          // 渲染app
          desktopController.Render(desktopController, taskbarController, microModule)
        }
      }
    }

    viewController.onResume {
      resumeState.value = true
    }

    viewController.onPause {
      resumeState.value = false
    }

    viewController.onDestroy {
      tabletopController?.activity = null
    }
  }
}
