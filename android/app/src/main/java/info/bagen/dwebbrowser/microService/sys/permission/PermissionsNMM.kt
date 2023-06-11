package info.bagen.dwebbrowser.microService.sys.permission

import info.bagen.dwebbrowser.microService.core.BootstrapContext
import info.bagen.dwebbrowser.microService.core.NativeMicroModule
import info.bagen.dwebbrowser.microService.helper.Mmid
import org.dweb_browser.helper.*
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

inline fun debugPermission(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("Permissions", tag, msg, err)

class PermissionsNMM : NativeMicroModule("permission.sys.dweb") {
    /** 存储每个微应用的权限*/
    private val permissionMap = mutableMapOf<Mmid, MutableList<Mmid>>()

    /** 存储系统的权限*/
    private val systemPermissions = mutableMapOf<Mmid, MutableList<String>>()

    companion object {
        val permission_op = PromiseOut<Boolean>()
    }

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        apiRouting = routes(
            /** 申请权限*/
            "/apply" bind Method.GET to defineHandler { request, ipc ->
                val permissions = ArrayList(Query.string().multi.required("permission")(request))
                applyPermissions(permissions, ipc.remote.mmid)
                // TODO 向用户申请之后这里应该有回调
                // permission_op
                return@defineHandler Response(Status.OK)
            },
            /** 查询是否有该权限，如果没有会向用户申请该权限*/
            "/query" bind Method.GET to defineHandler { request, ipc ->
                val permission = Query.string().required("permission")(request)
                val res = permissionMap[ipc.remote.mmid]?.contains(permission) ?: false
                if (!res) {
                    applyPermission(permission, ipc.remote.mmid)
                }
                val response = """{"hasPermission":${res}}"""
                Response(Status.OK).body(response)
            },
        )
    }

    private fun applyPermission(permission: String, mmid: Mmid) {

    }

    private fun applyPermissions(permissions: ArrayList<String>, mmid: Mmid) {

    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}