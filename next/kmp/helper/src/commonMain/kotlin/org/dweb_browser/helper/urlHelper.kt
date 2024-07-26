package org.dweb_browser.helper

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.decodeURLPart
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
fun String.toIpcUrl(builder: (URLBuilder.() -> Unit)? = null) =
  (if (startsWith(fileProtocol.name + ":")) {
    URLBuilder(this).apply {
      val index = indexOf("?")
      if (index != -1) {
        parameters.appendAll(parseQueryString(substring(index + 1)))
      }
    }
  } else if (startsWith(dwebProtocol.name + ":")) {
    URLBuilder("${dwebProtocol.name}://" + this.replaceFirst(':', '/'))
  } else URLBuilder(this)).run {
    builder?.invoke(this)
    build()
  }

fun buildUrlString(url: String, builder: URLBuilder.() -> Unit) = URLBuilder(url).run {
  builder();
  buildUnsafeString()
}


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
  if (path.isBlank() || path.isEmpty()) {
    return
  }
  val segments = path.split("/")
    .map { it.decodeURLPart() }
    .filter { it != "" && it != "." }
    .toMutableList()
  while (segments.contains("..")) {
    val index = segments.indexOf("..")
    segments.removeAt(index)
    val preIndex = index - 1
    if (preIndex >= 0) {
      segments.removeAt(preIndex)
    }
  }

  if (segments.firstOrNull() == "") {
    pathSegments = segments.filterNot { it == "" }
    return
  }
  pathSegments += segments.filterNot { it == "" }
}

fun String.toWebUrl() = try {
  val url = Url(this)
  if (url.toString().startsWith(this)) url else null
} catch (_: Throwable) {
  null
}

// 由于 isRealDomain() 走 DNS 校验，耗时较长，这边改为 isMaybeDomain()
fun String.toNoProtocolWebUrl() =
  if (this.split("/").first().isMaybeDomain()) "https://$this".toWebUrl() else null

/**尝试转换成webUrl*/
fun String.toWebUrlOrWithoutProtocol() = toWebUrl() ?: toNoProtocolWebUrl()

/**
 * 判断输入内容是否是域名或者有效的网址
 * 基于ktor的UrlBuilder函数，支持 http https ws wss socks 这几种协议( ftp/sftp 也许支持)
 */
fun String.isWebUrl() = toWebUrl() != null
fun String.isNoProtocolWebUrl() = toNoProtocolWebUrl() != null
fun String.isWebUrlOrWithoutProtocol() = toWebUrlOrWithoutProtocol() != null

fun String.isDwebDeepLink() = try {
  URLBuilder(this).buildUnsafeString().startsWith("dweb://")
} catch (_: Throwable) {
  false
}