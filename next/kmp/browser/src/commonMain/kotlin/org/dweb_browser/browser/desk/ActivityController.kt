package org.dweb_browser.browser.desk

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dweb_browser.browser.desk.model.ActivityItem
import org.dweb_browser.core.module.MicroModule

class ActivityController(val deskNMM: DeskNMM.DeskRuntime) {
  private val listFlow = MutableStateFlow(emptyList<ActivityItem>())
  val list = listFlow.asStateFlow()

  fun request(activityItem: ActivityItem): String {
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
    leadingIcon: ActivityItem.Icon?,
    trailingIcon: ActivityItem.Icon?,
    centerTitle: ActivityItem.Content?,
    bottomActions: List<ActivityItem.Action>?,
  ): Boolean {
    val activityItem = find(owner, id) ?: return false
    val newActivityItem = activityItem.run {
      copy(
        leadingIcon = leadingIcon ?: this.leadingIcon,
        trailingIcon = trailingIcon ?: this.trailingIcon,
        centerTitle = centerTitle ?: this.centerTitle,
        bottomActions = bottomActions ?: this.bottomActions,
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