package org.dweb_browser.core.std.file.ext

import okio.Path.Companion.toPath
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod

suspend fun MicroModule.Runtime.copyFile(sourcePath: String, targetPath: String) = nativeFetch(
  buildUrlString("file://file.std.dweb/copy") {
    parameters["sourcePath"] = sourcePath
    parameters["targetPath"] = targetPath
  }
).boolean()

suspend fun MicroModule.Runtime.moveFile(sourcePath: String, targetPath: String) = nativeFetch(
  buildUrlString("file://file.std.dweb/move") {
    parameters["sourcePath"] = sourcePath
    parameters["targetPath"] = targetPath
  }
).boolean()

suspend fun MicroModule.Runtime.removeFile(path: String) = nativeFetch(
  PureClientRequest(
    href = buildUrlString("file://file.std.dweb/remove") {
      parameters["path"] = path
      parameters["recursive"] = "true"
    },
    method = PureMethod.DELETE
  )
).boolean()

suspend fun MicroModule.Runtime.existFile(path: String) = nativeFetch(
  buildUrlString("file://file.std.dweb/exist") {
    parameters["path"] = path
  }
).boolean()

suspend fun MicroModule.Runtime.readFile(path: String, create: Boolean = false) = nativeFetch(
  buildUrlString("file://file.std.dweb/read") {
    parameters["path"] = path
    parameters["create"] = create.toString()
  }
)

suspend fun MicroModule.Runtime.infoFile(path: String) = nativeFetch(
  buildUrlString("file://file.std.dweb/info") {
    parameters["path"] = path
  }
).text()

suspend fun MicroModule.Runtime.pickFile(path: String) = nativeFetch(
  buildUrlString("file://file.std.dweb/picker") {
    parameters["path"] = path
  }
).text()

suspend fun MicroModule.Runtime.realPath(path: String) = nativeFetch(
  buildUrlString("file://file.std.dweb/realPath") {
    parameters["path"] = path
  }
).text().toPath()

suspend fun MicroModule.Runtime.appendFile(path: String, body: IPureBody) {
  nativeFetch(
    PureClientRequest(
      href = buildUrlString("file://file.std.dweb/append") {
        parameters["path"] = path
        parameters["create"] = "true"
      }, method = PureMethod.PUT, body = body
    )
  )
}

suspend fun MicroModule.Runtime.writeFile(
  path: String,
  body: IPureBody,
  create: Boolean = true,
  backup: Boolean = false,
) {
  nativeFetch(
    PureClientRequest(
      href = buildUrlString("file://file.std.dweb/write") {
        parameters["path"] = path
        parameters["create"] = "$create"
        parameters["backup"] = "$backup"
      }, method = PureMethod.POST, body = body
    )
  )
}

suspend fun MicroModule.Runtime.createDir(path: String) = nativeFetch(
  method = PureMethod.POST,
  url = buildUrlString("file://file.std.dweb/createDir") {
    parameters["path"] = path
  }
).boolean()

suspend fun MicroModule.Runtime.listDir(path: String) = nativeFetch(
  method = PureMethod.POST,
  url = buildUrlString("file://file.std.dweb/listDir") {
    parameters["path"] = path
  }
).json<List<String>>()
