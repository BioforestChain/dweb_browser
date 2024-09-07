package org.dweb_browser.browser.desk.render.activity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.browser.desk.model.rememberActivityStyle

@Composable
actual fun ActivityController.Render() {
  val activityStyle = rememberActivityStyle()
  Box(
    Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    if (DEV_ACTIVITY_CONTROLLER) {
      ActivityDevPanel(
        this@Render,
        Modifier
          .align(Alignment.BottomCenter)
          .padding(WindowInsets.safeGestures.asPaddingValues())
      )
    }
    CommonActivityListRender(this@Render, activityStyle)
  }
}

