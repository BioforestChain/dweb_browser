package org.dweb_browser.browser.desk.ext

import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.browser.desk.DeskNMM
import org.dweb_browser.browser.desk.model.ActivityItem
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.falseAlso

private suspend fun MicroModule.Runtime.getCurrentActivityController(): ActivityController {
  bootstrapContext.dns.open("activity.sys.dweb").falseAlso {
    throw Exception("activity.sys.dweb no working")
  }
  return DeskNMM.controllersMap.values.last().activityController
}

suspend fun MicroModule.Runtime.requestActivity(
  leadingIcon: ActivityItem.Icon = ActivityItem.NoneIcon,
  trailingIcon: ActivityItem.Icon,
  centerTitle: ActivityItem.Content,
  bottomActions: List<ActivityItem.Action> = emptyList(),
): String {
  return getCurrentActivityController().request(
    ActivityItem(
      owner = this,
      leadingIcon = leadingIcon,
      trailingIcon = trailingIcon,
      centerTitle = centerTitle,
      bottomActions = bottomActions,
    )
  )
}

suspend fun MicroModule.Runtime.updateActivity(
  id: String,
  leadingIcon: ActivityItem.Icon? = null,
  trailingIcon: ActivityItem.Icon? = null,
  centerTitle: ActivityItem.Content? = null,
  bottomActions: List<ActivityItem.Action>? = null,
): Boolean {
  return getCurrentActivityController().update(
    owner = this,
    id = id,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    centerTitle = centerTitle,
    bottomActions = bottomActions,
  )
}

suspend fun MicroModule.Runtime.endActivity(
  id: String,
): Boolean {
  return getCurrentActivityController().end(
    owner = this,
    id = id,
  )
}