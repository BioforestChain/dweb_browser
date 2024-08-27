package org.dweb_browser.browser.desk.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.util.fastForEach
import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.browser.desk.render.activity.ActivityItemRender
import org.dweb_browser.helper.compose.animation.LazyList

@Composable
expect fun ActivityController.Render()

@Composable
internal fun CommonActivityListRender(controller: ActivityController, paddingTop: Float) {
  val activityList by controller.list.collectAsState()
  LazyList(
    activityList,
    endAnimationFinished = { !it.renderProp.canView },
    playEndAnimation = { it.renderProp.open = false },
  ) { showList ->
    showList.fastForEach { activity ->
      key(activity.id) {
        ActivityItemRender(controller, activity, paddingTop)
      }
    }
  }
}