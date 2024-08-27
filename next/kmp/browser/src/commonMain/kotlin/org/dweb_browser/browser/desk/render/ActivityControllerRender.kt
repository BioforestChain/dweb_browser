package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.browser.desk.model.ActivityItem
import org.dweb_browser.browser.desk.model.ActivityStyle
import org.dweb_browser.browser.desk.render.activity.ActivityItemRender
import org.dweb_browser.helper.compose.animation.LazyList

@Composable
expect fun ActivityController.Render()

@Composable
internal fun CommonActivityListRender(
  controller: ActivityController,
  activityStyle: ActivityStyle,
) {
  val activityList by controller.list.collectAsState()
  LazyList(
    activityList,
    endAnimationFinished = { !it.renderProp.canView },
    playEndAnimation = { it.renderProp.open = false },
  ) { showList ->
    showList.fastForEach { activity ->
      key(activity.id) {
        ActivityItemRender(controller, activity, activityStyle)
      }
    }
  }
}

@Composable
internal fun ActivityDevPanel(controller: ActivityController, modifier: Modifier = Modifier) {
  Row(modifier) {
    val activityList = remember { mutableStateListOf<ActivityItem>() }
    Button(
      onClick = {
        controller.request(
          ActivityItem(
            owner = controller.deskNMM,
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
          controller.end(owner = owner, id = id)
          activityList -= this
        }
      },
      enabled = activityList.isNotEmpty()
    ) {
      Text(text = "销毁活动")
    }
  }
}