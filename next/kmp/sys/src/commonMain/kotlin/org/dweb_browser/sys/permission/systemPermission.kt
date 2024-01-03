package org.dweb_browser.sys.permission

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.AdapterManager
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.core.std.permission.debugPermission
import org.dweb_browser.helper.platform.IPureViewController

@Serializable
data class SystemPermissionTask(
  val name: SystemPermissionName,
  val title: String,
  val description: String = "",
)

typealias RequestSystemPermissionResult = Map<SystemPermissionName, AuthorizationStatus>

suspend fun requestSystemPermission(
  mm: MicroModule,
  pureViewController: IPureViewController?,
  permissionTaskList: List<SystemPermissionTask>
): Map<SystemPermissionName, AuthorizationStatus> {
  val result = (RequestSystemPermissionResult::toMutableMap)(mapOf())
  val restTasks = permissionTaskList.associateBy { it.name }.toMutableMap()
  debugPermission("requestSystemPermission", "names=${permissionTaskList.joinToString(",")}")
  for (adapter in systemPermissionAdapterManager.adapters) {
    for ((name, task) in restTasks.toList()) {
      when (val status =
        RequestSystemPermissionContext(
          mm,
          pureViewController,
          task,
          permissionTaskList
        ).adapter()) {
        null -> continue
        else -> {
          result[name] = status
          restTasks.remove(name)
        }
      }
    }
  }
  for (name in restTasks.keys) {
    result[name] = AuthorizationStatus.UNKNOWN
  }
  return result
}

@Suppress("ClassName")
object systemPermissionAdapterManager : AdapterManager<RequestSystemPermission>() {

}

typealias RequestSystemPermission = suspend RequestSystemPermissionContext.() -> AuthorizationStatus?

class RequestSystemPermissionContext(
  val mm: MicroModule,
  val pureViewController: IPureViewController?,
  val task: SystemPermissionTask,
  val names: List<SystemPermissionTask>
)