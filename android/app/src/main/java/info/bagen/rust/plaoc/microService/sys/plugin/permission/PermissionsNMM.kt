package info.bagen.rust.plaoc.microService.sys.plugin.permission

import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.helper.printdebugln
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.Jackson.auto
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

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext)
 {
        apiRouting = routes(
            /** 申请权限*/
            "/apply" bind Method.GET to defineHandler { request, ipc ->
                val permission = Query.string().optional("permission")(request)
                val permissions = Query.auto<ArrayList<String>>().optional("permissions")(request)
                // 必须传递一个或者多个权限，当传递多个权限时，以多个权限为主
                if (permission == null && permissions == null) {
                    return@defineHandler Response(Status.UNSATISFIABLE_PARAMETERS).body("At least one of permission or permissions must be transmission ")
                }
                if (permissions != null) {
                    applyPermissions(permissions,ipc.remote.mmid)
                } else if (permission != null) {
                    applyPermission(permission,ipc.remote.mmid)
                }
                // TODO 向用户申请之后这里应该有回调
                // permission_op
                return@defineHandler Response(Status.OK)
            },
            /** 查询是否有该权限，如果没有会向用户申请该权限*/
            "/query" bind Method.GET to defineHandler { request, ipc ->
                val permission = Query.string().required("permission")(request)
                val res = permissionMap[ipc.remote.mmid]?.contains(permission) ?: false
                if (!res) {
                    applyPermission(permission,ipc.remote.mmid)
                }
                val response = """{"hasPermission":${res}}"""
                Response(Status.OK).body(response)
            },
        )
    }

    private fun applyPermission(permission:String, mmid:Mmid) {
        App.browserActivity?.let {
            PermissionManager.requestPermissions(
                it,
                permission
            )
        }
        val list = systemPermissions[mmid] ?: mutableListOf()
        list.add(permission)
    }

    private fun applyPermissions(permissions: ArrayList<String>,mmid: Mmid) {
        App.browserActivity?.let {
            PermissionManager.requestPermissions(
                it,
                permissions
            )
        }
        //TODO 存一下，这里如果是数据库会好一点
        val list = systemPermissions[mmid] ?: mutableListOf()
        permissions.forEach {
            list.add(it)
        }
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}