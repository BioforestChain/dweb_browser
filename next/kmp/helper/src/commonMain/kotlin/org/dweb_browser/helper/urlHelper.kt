package org.dweb_browser.helper

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.encodedPath
import io.ktor.http.parseQueryString

//import io.ktor.http.Url

fun URLBuilder.buildUnsafeString(): String {
  val originProtocol = protocol
  return when (originProtocol.name) {
    fileProtocol.name -> {
      protocol = URLProtocol.HTTP
      buildString().replaceFirst(protocol.name, originProtocol.name)
    }

    dwebProtocol.name -> {
      buildString().replace("${dwebProtocol.name}://${dwebProtocol.name}/", "${dwebProtocol.name}:")
    }

    else -> buildString()
  }
}

val fileProtocol = URLProtocol.createOrDefault("file")
val dwebProtocol = URLProtocol.createOrDefault("dweb")
fun String.toIpcUrl() = if (startsWith(fileProtocol.name + ":")) {
  URLBuilder(this).run {
    val index = indexOf("?")
    if (index != -1) {
      parameters.appendAll(parseQueryString(substring(index + 1)))
    }
    build()
  }
} else if (startsWith(dwebProtocol.name + ":")) {
  URLBuilder("${dwebProtocol.name}://" + this.replaceFirst(':', '/')).build()
} else Url(this)


///**
// * like web url
// */
//data class PureUrl(
//  /**
//   * eg: "http", "https", "file", "dweb"
//   */
//  val protocol: String,
//  val user: String?,
//  val password: String?,
//  val host: String,
//  val port: Int,
//  val pathname: String,
//  val search: String,
//  val hash: String,
//) {
//  /**
//   * keep trailing question character even if there are no query parameters
//   */
//  val trailingQuery by lazy { search.isNotEmpty() }
//  val urlProtocol by lazy { URLProtocol.createOrDefault(protocol) }
//  val isDefaultPort get() = urlProtocol.defaultPort
//
//  /**
//   * full url, like "http://localhost:4454/xx"
//   */
//  val href by lazy {
//    val userInfo = "$user"
//    "$protocol:${user}"
//  }
//
//  fun toUrlBuilder() = when (protocol) {
//    /// file 协议下，补充 parameters
//    "file" -> URLBuilder(href).apply {
//      if (trailingQuery) {
//        parameters.appendAll(parseQueryString(search.substring(1)))
//      }
//    }
//    /// dweb 协议下，使用 https 作为 protocol，使用 dweb 作为 host
//    "dweb" -> URLBuilder("https://dweb/$pathname$search$hash")
//    else -> URLBuilder(href)
//  }
//
//  companion object {
////    fun URLBuilder.toPureUrl() = PureUrl()
//
//  }
//}

fun Url.build(block: URLBuilder.() -> Unit) = URLBuilder(this).run { block(); build() }

/**
 * 参考 [URLBuilder.encodedPath]
 */
fun URLBuilder.resolvePath(path: String) {
  if (path.isBlank()) {
    return
  }

  if(path.startsWith("./")) {
    encodedPath = path.replaceFirst(".", "")
    return
  }

  if (path.startsWith("/")) {
    encodedPath = path
    return
  }
  val basePathSegments =
    if (!path.endsWith("/")) pathSegments.toMutableList() else pathSegments.toMutableList()
      .also { it.removeLastOrNull() }

  val toPathSegments = path.split("/").toMutableList()
  for (part in toPathSegments.toList()) {
    if (part == ".") {
      toPathSegments.remove(part)
    } else if (part == "..") {
      toPathSegments.remove(part)
      basePathSegments.removeLastOrNull()
    } else {
      break
    }
  }
  pathSegments = basePathSegments + basePathSegments
}
