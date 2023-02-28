package info.bagen.rust.plaoc.microService.sys.plugin.permission

import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.printdebugln
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes

inline fun debugPermission(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("Permissions", tag, msg, err)

class PermissionsNMM:NativeMicroModule("permission.sys.dweb") {
    /** 存储每个微应用的权限*/
    val permissionMap = mutableMapOf<Mmid,MutableList<Mmid>>()
    /** 存储系统的权限*/
    val systemPermissions = mutableMapOf<Mmid,String>()

    override suspend fun _bootstrap() {
        apiRouting = routes(
            /** 申请权限*/
            "/apply" bind Method.GET to defineHandler { request ->
                Response(Status.OK)
            },
            /** 查询是否有该权限，如果没有会向用户申请该权限*/
            "/inquiry" bind Method.GET to defineHandler { request ->
                Response(Status.OK)
            },
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}