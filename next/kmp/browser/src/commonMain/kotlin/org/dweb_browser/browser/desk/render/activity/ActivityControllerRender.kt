package org.dweb_browser.browser.desk.render.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
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
import org.dweb_browser.helper.compose.animation.LazyList
import org.dweb_browser.helper.toFixed

/**
 * 如果你是开发者，可以开启这个常量，来做对ActivityController的调试
 */
internal const val DEV_ACTIVITY_CONTROLLER = false

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
internal fun ActivityDevPanel(
  controller: ActivityController,
  modifier: Modifier = Modifier
) {
  Column {
    Text("动画刚性 ➗${animationStiffness.toFixed(2)}")
    Slider(animationStiffness, { animationStiffness = it }, valueRange = 1f..200f)
    HorizontalDivider()
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
                CircularProgressIndicator(modifier)
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
}