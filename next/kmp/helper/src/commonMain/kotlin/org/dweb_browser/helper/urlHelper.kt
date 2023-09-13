package org.dweb_browser.helper

import io.ktor.http.DEFAULT_PORT
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url

//import io.ktor.http.Url

fun URLBuilder.buildUnsafeString(): String {
  val originProtocol = protocol
  return if (originProtocol.name == "file") {
    protocol = URLProtocol.HTTP
    buildString().replaceFirst(protocol.name, originProtocol.name)
  } else buildString()
}

fun String.keepFileParameters(): Url {
  val tmpUrl = Url(this)

  return URLBuilder(
    tmpUrl.protocol,
    tmpUrl.host,
    tmpUrl.port,
    tmpUrl.user,
    tmpUrl.password,
    tmpUrl.pathSegments,
    Url(this.replaceFirst("file", "http")).parameters,
    tmpUrl.fragment,
    tmpUrl.trailingQuery
  ).build()
}