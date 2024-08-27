package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.browser.desk.model.ActivityItem
import kotlin.math.max

@Composable
actual fun ActivityController.Render() {
  val density = LocalDensity.current
  val paddingTop = max(WindowInsets.safeGestures.getTop(density) / density.density, 32f)
  Box(
    Modifier
      .fillMaxSize()
      .padding(top = paddingTop.dp),
    contentAlignment = Alignment.TopCenter,
  ) {
    Row(Modifier.align(Alignment.BottomCenter)) {
      val activityList = remember { mutableStateListOf<ActivityItem>() }
      Button(
        onClick = {
          request(
            ActivityItem(
              owner = deskNMM,
              leadingIcon = ActivityItem.ComposeIcon { modifier ->
                Icon(Icons.Rounded.Downloading, contentDescription = null, modifier = modifier)
              },
              trailingIcon = ActivityItem.ComposeIcon { modifier ->
                CircularProgressIndicator(
                  modifier
                )
              },
              centerTitle = ActivityItem.TextContent("Hello Gaubee, This is Long Text!!!"),
              bottomActions = emptyList(),
            ).also { activityList += it }
          )
        },
        enabled = activityList.isEmpty()
      ) {
        Text(text = "创建活动")
      }
      Button(
        onClick = {
          activityList.first().apply {
            end(owner = owner, id = id)
            activityList -= this
          }
        },
        enabled = activityList.isNotEmpty()
      ) {
        Text(text = "销毁活动")
      }
    }
    CommonActivityListRender(this@Render, paddingTop)
  }
}
