package org.dweb_browser.microservice.ipc.helper

import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.StringEnumSerializer

object IpcMethodSerializer :
  StringEnumSerializer<IpcMethod>("MatchMode", IpcMethod.ALL_VALUES, { method })

/**
 * Ipc 使用的 Method
 */
@Serializable(IpcMethodSerializer::class)
enum class IpcMethod(val method: String, val ktorMethod: HttpMethod) {
  GET("GET", HttpMethod.Get),
  POST("POST", HttpMethod.Post),
  PUT("PUT", HttpMethod.Put),
  DELETE("DELETE", HttpMethod.Delete),
  OPTIONS("OPTIONS", HttpMethod.Options),
  TRACE("TRACE", HttpMethod.Post),
  CONNECT("CONNECT", HttpMethod.Post),
  PATCH("PATCH", HttpMethod.Patch),
  PURGE("PURGE", HttpMethod.Post),
  HEAD("HEAD", HttpMethod.Head),
  ;

  companion object {
    val ALL_VALUES = IpcMethod.entries.associateBy { it.method }
    fun from(ktorMethod: HttpMethod) = entries.first { it.ktorMethod == ktorMethod }
  }
}