package org.dweb_browser.core.std.permission

import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonPrimitive
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.NativeMicroModule

/**
 * 权限模块需要附着到原生模块上才能完整，这里只提供一些基本标准
 */
suspend fun NativeMicroModule.permissionStdProtocol() {
  protocol("permission.std.dweb") {
    routes(
      /// 查询某些权限是否拥有过
      "/query" bind HttpMethod.Get to defineJsonResponse {
        JsonPrimitive("qaq")
      },
      /// 申请某些权限
      //      "/" bind HttpMethod.Get
    )
  }
}