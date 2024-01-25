package org.dweb_browser.pure.http

import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.StringEnumSerializer

object PureMethodSerializer :
  StringEnumSerializer<PureMethod>("MatchMode", PureMethod.ALL_VALUES, { method })

/**
 * Ipc 使用的 Method
 */
@Serializable(PureMethodSerializer::class)
enum class PureMethod(val method: String, val ktorMethod: HttpMethod) {
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
    val ALL_VALUES = PureMethod.entries.associateBy { it.method }
    fun from(ktorMethod: HttpMethod) = from(ktorMethod.value)
    fun from(method: String) = ALL_VALUES[method.uppercase()] ?: GET
  }
}