package org.dweb_browser.helper

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.encodedPath
import io.ktor.http.parseQueryString
import okio.Path.Companion.toPath

//import io.ktor.http.Url
public fun URLBuilder.buildUnsafeString(): String {
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

public val fileProtocol: URLProtocol = URLProtocol.createOrDefault("file")
public val dwebProtocol: URLProtocol = URLProtocol.createOrDefault("dweb")
public fun String.toIpcUrl(builder: (URLBuilder.() -> Unit)? = null): Url =
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

public fun buildUrlString(url: String, builder: URLBuilder.() -> Unit): String =
  URLBuilder(url).run {
    builder()
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
public fun Url.build(block: URLBuilder.() -> Unit): Url = URLBuilder(this).run { block(); build() }

public fun URLBuilder.resolveBaseUri() {
  if (!encodedPath.endsWith("/")) {
    resolvePath("../")
  }
}

/**
 * 参考 [URLBuilder.encodedPathSegments]
 */
public fun URLBuilder.resolvePath(path: String) {
  if (path.isBlank() || path.isEmpty()) {
    return
  }
  encodedPath = encodedPath.toPath().resolve(path, true).toString()
//  val segments = path.split("/").map { it.decodeURLPart() }.filter { it != "." }.toMutableList()
//  while (segments.contains("..")) {
//    val index = segments.indexOf("..")
//    segments.removeAt(index)
//    val preIndex = index - 1
//    if (preIndex >= 0) {
//      segments.removeAt(preIndex)
//    }
//  }
//
//  if (segments.firstOrNull() == "") {
//    pathSegments = segments.filterNot { it == "" }
//    return
//  }
//  pathSegments += segments.filterNot { it == "" }
}

public fun String.toWebUrl(): Url? = try {
  val url = Url(this)
  if (url.toString().startsWith(this)) url else null
} catch (_: Throwable) {
  null
}

// 由于 isRealDomain() 走 DNS 校验，耗时较长，这边改为 isMaybeDomain()
public fun String.toNoProtocolWebUrl(): Url? =
  if (this.split("/").first().isMaybeDomain()) "https://$this".toWebUrl() else null

/**尝试转换成webUrl*/
public fun String.toWebUrlOrWithoutProtocol(): Url? = toWebUrl() ?: toNoProtocolWebUrl()

/**
 * 判断输入内容是否是域名或者有效的网址
 * 基于ktor的UrlBuilder函数，支持 http https ws wss socks 这几种协议( ftp/sftp 也许支持)
 */
public fun String.isWebUrl(): Boolean = toWebUrl() != null
public fun String.isNoProtocolWebUrl(): Boolean = toNoProtocolWebUrl() != null

public fun String.isWebUrlOrWithoutProtocol(): Boolean = when {
  // 如果是空白字符，那么就不是网址
  isBlank() -> false
  // 如果是是存在空格，那么就不是网址
  contains(Regex("\\s")) -> false
  else -> toWebUrlOrWithoutProtocol() != null
}

public fun String.isDwebDeepLink(): Boolean = try {
  URLBuilder(this).buildUnsafeString().startsWith("dweb://")
} catch (_: Throwable) {
  false
}