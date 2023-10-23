package info.bagen.dwebbrowser.microService.sys.permission

import android.os.Bundle
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.util.permission.PermissionUtil
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.types.MMID
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

fun debugPermission(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("Permissions", tag, msg, err)

class PermissionsNMM : NativeMicroModule("permission.sys.dweb", "permission") {

  init {
    name = "Permissions"
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);
  }

  /** 存储每个微应用的权限*/
  private val permissionMap = mutableMapOf<MMID, MutableList<MMID>>()

  /** 存储系统的权限*/
  private val systemPermissions = mutableMapOf<MMID, MutableList<String>>()

  companion object {
    val permission_op = PromiseOut<Boolean>()
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    apiRouting = routes(
      /** 申请权限*/
      "/apply" bind Method.GET to defineHandler { request, ipc ->
        debugPermission("apply", "apply permission")
        val permissions = ArrayList(Query.string().multi.required("permission")(request))
        applyPermissions(permissions, ipc.remote.mmid)
        // TODO 向用户申请之后这里应该有回调
        // permission_op
        return@defineHandler Response(Status.OK)
      },
      /** 查询是否有该权限，如果没有会向用户申请该权限*/
      "/query" bind Method.GET to defineBooleanResponse {
        val permission = Query.string().required("permission")(request)
        debugPermission("query", "permission=$permission")
        permissionMap[ipc.remote.mmid]?.contains(permission) ?: run {
          requestPermissionByActivity(permission)
        }
        /*val res = permissionMap[ipc.remote.mmid]?.contains(permission) ?: false
        if (!res) {
          applyPermission(permission, ipc.remote.mmid)
        }
        val response = """{"hasPermission":${res}}"""
        Response(Status.OK).body(response)*/
      },
    )
  }

  private fun applyPermissions(permissions: ArrayList<String>, mmid: MMID) {
  }

  private suspend fun requestPermissionByActivity(permission: String): Boolean {
    val permissions = PermissionUtil.getActualPermissions(permission)
    debugPermission("requestPermissionByActivity", "permissions = $permissions")
    App.startActivity(PermissionActivity::class.java) { intent ->
      intent.putExtras(Bundle().also { it.putStringArrayList("permissions", permissions) })
    }
    return PermissionController.controller.waitGrantResult()
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}