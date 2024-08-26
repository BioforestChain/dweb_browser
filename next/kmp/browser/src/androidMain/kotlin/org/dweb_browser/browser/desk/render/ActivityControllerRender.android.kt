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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.browser.desk.model.ActivityItem
import org.dweb_browser.browser.desk.render.activity.ActivityItemRender
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
    val activityList by list.collectAsState()
//    var _preShowList by remember { mutableStateOf<List<ActivityItem>>(emptyList()) }
    var _showList by remember { mutableStateOf<List<ActivityItem>>(emptyList()) }

    val preShowList = _showList.toMutableList()
    val showList = mutableListOf<ActivityItem>()
    for (index in activityList.indices.reversed()) {
      val activity = activityList[index]
      if (!activity.renderProp.canView) {
        break
      }
      showList += activity
      if (preShowList.contains(activity)) {
        preShowList -= activity
      }
    }
    preShowList.fastForEach { activity ->
      if (activity.renderProp.open || activity.renderProp.viewAniRunning) {
        activity.renderProp.open = false
        showList += activity
      } else {
        showList -= activity
      }
    }
    showList.fastForEachReversed { activity ->
      key(activity.id) {
        ActivityItemRender(this@Render, activity, paddingTop)
      }
    }
    if (!_showList.containsAll(showList)) {
      _showList = showList
    }
  }
}