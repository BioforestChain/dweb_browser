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
    leadingIcon: ActivityItem.Icon? = null,
    trailingIcon: ActivityItem.Icon? = null,
    centerTitle: ActivityItem.Content? = null,
    bottomActions: List<ActivityItem.Action>? = null,
  ): Boolean {
    val activityItem = find(owner, id) ?: return false
    leadingIcon?.also { activityItem.leadingIcon = leadingIcon }
    trailingIcon?.also { activityItem.trailingIcon = trailingIcon }
    centerTitle?.also { activityItem.centerTitle = centerTitle }
    bottomActions?.also { activityItem.bottomActions = bottomActions }
    // 修改后，强制进行显示。可以考虑挪到最前面
    activityItem.renderProp.open = true
    return true
  }

  fun end(owner: MicroModule.Runtime, id: String): Boolean {
    val activityItem = find(owner, id) ?: return false
    listFlow.value -= activityItem
    return true
  }
}