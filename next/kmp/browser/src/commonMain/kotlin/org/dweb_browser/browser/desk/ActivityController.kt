package org.dweb_browser.browser.desk

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dweb_browser.browser.desk.model.ActivityItem
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.randomUUID

class ActivityController(val deskNMM: DeskNMM.DeskRuntime) {
  private val listFlow = MutableStateFlow(emptyList<ActivityItem>())
  val list = listFlow.asStateFlow()
  fun request(
    owner: MicroModule.Runtime,
    icon: ActivityItem.Icon,
    content: ActivityItem.Content,
    action: ActivityItem.Action,
  ): String {
    val activityItem = ActivityItem(
      id = randomUUID(),
      owner = owner,
      icon = icon,
      content = content,
      action = action,
    )
    listFlow.value += activityItem

    return activityItem.id
  }

  private fun find(owner: MicroModule.Runtime, id: String): ActivityItem? {
    val activityItem = listFlow.value.find { it.id == id } ?: return null
    if (activityItem.owner.id != owner.id) return null
    return activityItem
  }

  fun update(
    owner: MicroModule.Runtime,
    id: String,
    icon: ActivityItem.Icon?,
    content: ActivityItem.Content?,
    action: ActivityItem.Action?,
  ): Boolean {
    val activityItem = find(owner, id) ?: return false
    val newActivityItem = activityItem.run {
      copy(
        icon = icon ?: this.icon,
        content = content ?: this.content,
        action = action ?: this.action,
      )
    }
    listFlow.value = listFlow.value - activityItem + newActivityItem
    return true
  }

  fun end(owner: MicroModule.Runtime, id: String): Boolean {
    val activityItem = find(owner, id) ?: return false
    listFlow.value -= activityItem
    return true
  }
}