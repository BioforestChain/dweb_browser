package org.dweb_browser.core.std.file.ext

import okio.Path.Companion.toPath
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureBinaryBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse

suspend fun MicroModule.Runtime.copyFile(sourcePath: String, targetPath: String) = nativeFetch(
  "file://file.std.dweb/copy?sourcePath=${sourcePath}&targetPath=${targetPath}"
).boolean()

suspend fun MicroModule.Runtime.moveFile(sourcePath: String, targetPath: String) = nativeFetch(
  "file://file.std.dweb/move?sourcePath=${sourcePath}&targetPath=${targetPath}"
).boolean()

suspend fun MicroModule.Runtime.removeFile(path: String) = nativeFetch(
  PureClientRequest(
    "file://file.std.dweb/remove?path=${path}&recursive=true", PureMethod.DELETE
  )
).boolean()

suspend fun MicroModule.Runtime.existFile(path: String) = nativeFetch(
  "file://file.std.dweb/exist?path=$path"
).boolean()

suspend fun MicroModule.Runtime.readFile(path: String, create: Boolean = false) = nativeFetch(
  "file://file.std.dweb/read?path=$path&create=$create"
)

suspend fun MicroModule.Runtime.infoFile(path: String) = nativeFetch(
  "file://file.std.dweb/info?path=$path"
).text()

suspend fun MicroModule.Runtime.pickFile(path: String) = nativeFetch(
  "file://file.std.dweb/picker?path=$path"
).text()

suspend fun MicroModule.Runtime.realPath(path: String) = nativeFetch(
  "file://file.std.dweb/realPath?path=$path"
).text().toPath()

suspend fun MicroModule.Runtime.appendFile(path: String, body: IPureBody) {
  nativeFetch(
    PureClientRequest(
      "file://file.std.dweb/append?path=${path}&create=true", PureMethod.PUT, body = body
    )
  )
}

suspend fun MicroModule.Runtime.writeFile(path: String, body: IPureBody) {
  nativeFetch(
    PureClientRequest(
      "file://file.std.dweb/write?path=${path}&create=true", PureMethod.POST, body = body
    )
  )
}

suspend fun MicroModule.Runtime.createDir(path: String) =
  nativeFetch(PureMethod.POST, "file://file.std.dweb/createDir?path=$path").boolean()

suspend fun MicroModule.Runtime.listDir(path: String) =
  nativeFetch(PureMethod.POST, "file://file.std.dweb/listDir?path=$path").json<List<String>>()

suspend fun MicroModule.Runtime.blobWrite(data: ByteArray, mime: String = "", ext: String = "") =
  nativeFetch(
    PureClientRequest(
      "file://file.std.dweb/blob/write?mime=$mime&ext=$ext",
      PureMethod.POST,
      body = PureBinaryBody(data)
    )
  ).text()