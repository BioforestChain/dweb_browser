package org.dweb_browser.browser.desk

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.create
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme
import org.dweb_browser.sys.window.core.Rect

fun Rect.toModifier(
  modifier: Modifier = Modifier,
) = modifier
  .offset(x.dp, y.dp)
  .size(width.dp, height.dp)

class DesktopViewControllerCore(val controller: IPureViewController) {
  private var desktopController: DesktopController? = null
  private fun bindController(sessionId: String?): DeskNMM.Companion.DeskControllers {
    /// 解除上一个 controller的activity绑定
    desktopController?.activity = null

    return DeskNMM.controllersMap[sessionId]?.also { controllers ->
      controllers.desktopController.activity = controller
      this.desktopController = controllers.desktopController
      controllers.activityPo.resolve(IPureViewBox.create(controller))
    } ?: throw Exception("no found controller by sessionId: $sessionId")
  }

  init {
    controller.onCreate { params ->
      val (desktopController, taskbarController, microModule) = bindController(params.getString("deskSessionId"))

      controller.addContent {
        DwebBrowserAppTheme {
          desktopController.Render(taskbarController, microModule)
        }
      }
    }

    controller.onDestroy {
      desktopController?.activity = null
    }
  }
}
