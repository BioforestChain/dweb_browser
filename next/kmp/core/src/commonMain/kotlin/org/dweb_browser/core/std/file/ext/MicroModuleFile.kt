package org.dweb_browser.core.std.file.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod

suspend fun MicroModule.copyFile(sourcePath: String, targetPath: String) = nativeFetch(
  "file://file.std.dweb/copy?sourcePath=${sourcePath}&targetPath=${targetPath}"
).boolean()

suspend fun MicroModule.moveFile(sourcePath: String, targetPath: String) = nativeFetch(
  "file://file.std.dweb/move?sourcePath=${sourcePath}&targetPath=${targetPath}"
).boolean()

suspend fun MicroModule.removeFile(path: String) = nativeFetch(
  PureClientRequest(
    "file://file.std.dweb/remove?path=${path}&recursive=true", PureMethod.DELETE
  )
).boolean()

suspend fun MicroModule.existFile(path: String) = nativeFetch(
  "file://file.std.dweb/exist?path=$path"
).boolean()

suspend fun MicroModule.readFile(path: String, create: Boolean = false) = nativeFetch(
  "file://file.std.dweb/read?path=$path&create=$create"
)

suspend fun MicroModule.infoFile(path: String) = nativeFetch(
  "file://file.std.dweb/info?path=$path"
).text()

suspend fun MicroModule.pickFile(path: String) = nativeFetch(
  "file://file.std.dweb/picker?path=$path"
).text()

suspend fun MicroModule.realFile(path: String) = nativeFetch(
  "file://file.std.dweb/realPath?path=$path"
).text()

suspend fun MicroModule.appendFile(path: String, body: IPureBody) {
  nativeFetch(
    PureClientRequest(
      "file://file.std.dweb/append?path=${path}&create=true",
      PureMethod.PUT,
      body = body
    )
  )
}

suspend fun MicroModule.writeFile(path: String, body: IPureBody) {
  nativeFetch(
    PureClientRequest(
      "file://file.std.dweb/write?path=${path}&create=true",
      PureMethod.POST,
      body = body
    )
  )
}