package org.dweb_browser.helper

import io.ktor.http.ParametersBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.parametersOf
import io.ktor.http.parseQueryString

//import io.ktor.http.Url

fun URLBuilder.buildUnsafeString(): String {
  val originProtocol = protocol
  return if (originProtocol.name == "file") {
    protocol = URLProtocol.HTTP
    buildString().replaceFirst(protocol.name, originProtocol.name)
  } else buildString()
}

val ipcProtocol = URLProtocol.createOrDefault("file")
fun String.toIpcUrl(): Url {
  val isIpcProtocol = startsWith(ipcProtocol.name)
  return URLBuilder(this).run {
    if (isIpcProtocol) {
      val index = indexOf("?")
      if (index != -1) {
        parameters.appendAll(parseQueryString(substring(index + 1)))
      }
    }
    build()
  }
}