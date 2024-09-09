package org.dweb_browser.browser.desk.render.activity

import androidx.compose.runtime.Composable
import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.LocalPureViewController
import org.dweb_browser.helper.platform.asDesktop

@Composable
actual fun ActivityController.Render() {
  val avc = activityControllerPvcWM.getOrPut(this) {
    ActivityViewController(
      this,
      LocalPureViewController.current.asDesktop().composeWindowAsState().value
    )
  }
  avc.Launch()
  /// 调试面板
  if (DEV_ACTIVITY_CONTROLLER) {
    avc.devParams.Render(avc)
  }
}

private val activityControllerPvcWM = WeakHashMap<ActivityController, ActivityViewController>()
