package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.browser.desk.model.rememberActivityStyle
import org.dweb_browser.helper.platform.rememberDisplaySize

@Composable
actual fun ActivityController.Render() {
//  val activityStyle = rememberActivityStyle()
  Box(
    Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    if (true) {
      ActivityDevPanel(
        this@Render,
        Modifier
          .align(Alignment.BottomCenter)
          .padding(WindowInsets.safeGestures.asPaddingValues())
      )
    }
//    CommonActivityListRender(this@Render, activityStyle)
  }

  val controller = this
  val activityStyle = rememberActivityStyle()
  val displaySize = rememberDisplaySize()
  Box(modifier = Modifier.size(1.dp)) {
    Layout(content = {
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        CommonActivityListRender(controller, activityStyle)
      }
    }) { measurables, constraints ->
      println("QAQ constraints=$constraints")
      // MEASUREMENT SCOPE
      val placeables = measurables.map { measurable ->
        measurable.measure(
          constraints.copy(
            maxWidth = (displaySize.width * density).toInt(),
            maxHeight = (displaySize.height * density).toInt()
          )
        )
      }
      println("QAQ placeables=${placeables.fastJoinToString { "${it.width}x${it.height}" }}")
      layout(constraints.maxWidth, constraints.maxHeight) {
        placeables.forEach {
          it.place(0, 0)
        }
      }
    }
  }
}
