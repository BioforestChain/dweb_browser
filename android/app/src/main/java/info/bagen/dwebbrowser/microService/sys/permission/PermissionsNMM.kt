package info.bagen.dwebbrowser.microService.sys.permission

import android.os.Bundle
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.printDebug
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.startAppActivity

fun debugPermission(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("Permissions", tag, msg, err)

class PermissionsNMM : NativeMicroModule("permission.sys.dweb", "permission") {

  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Protocol_Service);
  }

  /** 存储每个微应用的权限*/
  private val permissionMap = mutableMapOf<MMID, MutableList<MMID>>()

  /** 存储系统的权限*/
  private val systemPermissions = mutableMapOf<MMID, MutableList<String>>()

  companion object {
    val permission_op = PromiseOut<Boolean>()
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      /** 申请权限*/
      "/apply" bind HttpMethod.Get to definePureResponse {
//        val permissions = ArrayList(Query.string().multi.required("permission")(request))
//        applyPermissions(permissions, ipc.remote.mmid)
//        // TODO 向用户申请之后这里应该有回调
//        // permission_op
//        return@definePureResponse Response(Status.OK)
        return@definePureResponse PureResponse(
          HttpStatusCode.OK
        )
      },
      /** 查询是否有该权限，如果没有会向用户申请该权限*/
      "/query" bind HttpMethod.Get to defineBooleanResponse {
        val permission = request.query("permission")
        debugPermission("query", "permission=$permission")
        permissionMap[ipc.remote.mmid]?.contains(permission) ?: run {
          requestPermissionByActivity(permission)
        }
      },
    )
  }

  private suspend fun requestPermissionByActivity(permission: String): Boolean {
    val permissions = getActualPermissions(permission)
    debugPermission("requestPermissionByActivity", "permissions = $permissions")
    startAppActivity(PermissionActivity::class.java) { intent ->
      intent.putExtras(Bundle().also { it.putStringArrayList("permissions", permissions) })
      PermissionController.controller.granted = null
    }
    return PermissionController.controller.waitGrantResult()
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}