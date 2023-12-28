package org.dweb_browser.sys.share

import io.ktor.http.ContentType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.receiveMultipart
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.platform.MultiPartFile
import org.dweb_browser.helper.platform.MultiPartFileEncode
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.toBase64ByteArray
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.helper.toUtf8ByteArray

fun debugShare(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("Share", tag, msg, err)

@Serializable
data class ShareResult(val success: Boolean, val message: String)

@Serializable
data class ShareOptions(
  val title: String?,
  val text: String?,
  val url: String?,
)

class ShareNMM : NativeMicroModule("share.sys.dweb", "share") {
  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Protocol_Service);
  }

  @OptIn(ExperimentalSerializationApi::class)
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      /** 分享*/
      "/share" bind PureMethod.POST by defineJsonResponse {
        val contentType =
          request.headers.get("Content-Type")?.let { ContentType.parse(it) } ?: ContentType.Any

        val shareOptions = ShareOptions(
          title = request.queryOrNull("title"),
          text = request.queryOrNull("text"),
          url = request.queryOrNull("url"),
        )
        debugShare("share", "contentType=$contentType, shareOption=$shareOptions")
        val result = when {
          contentType.match(ContentType.MultiPart.FormData) ->
            try {
              share(shareOptions, request.receiveMultipart(), this@ShareNMM)
            } catch (e: Exception) {
              debugShare("ContentType/Form", "receiveMultipart error -> ${e.message}")
              share(shareOptions, null)
            }

          contentType.match(ContentType.Application.Json) -> {
            val files = Json.decodeFromString<List<MultiPartFile>>(request.body.toPureString())
            val fileList = mutableListOf<String>()

            files.forEach {
              fileList.add(multipartFileDataWriteToTempFile(it, ipc.remote.mmid))
            }

            share(shareOptions, fileList, this@ShareNMM)
          }

          contentType.match(ContentType.Application.Cbor) -> {
            val byteArray = request.body.toPureBinary()
            debugShare("ContentType/Cbor", "byteArray = ${byteArray.size}")

            val files = Cbor.decodeFromByteArray<List<MultiPartFile>>(byteArray)
            val fileList = mutableListOf<String>()

            files.forEach {
              fileList.add(multipartFileDataWriteToTempFile(it, ipc.remote.mmid))
            }

            share(shareOptions, fileList, this@ShareNMM)
          }

          else -> {
            debugShare("share", "Unable to process $contentType")
            /*return@defineJsonResponse ShareResult(
              false,
              "Unable to process $contentType"
            ).toJsonElement()*/
            share(shareOptions, null)
          }
        }

        debugShare("/share", "result => $result")
        ShareResult(result == "OK", result).toJsonElement()
      },
    ).cors()
  }

  private suspend fun multipartFileDataWriteToTempFile(
    multiPartFile: MultiPartFile,
    mmid: MMID
  ): String {
    val data = when (multiPartFile.encoding) {
      MultiPartFileEncode.UTF8 -> multiPartFile.data.toUtf8ByteArray()
      MultiPartFileEncode.BASE64 -> multiPartFile.data.toBase64ByteArray()
      MultiPartFileEncode.BINARY -> multiPartFile.data.toUtf8ByteArray()
    }

    val writePath = "/cache/${mmid}/${randomUUID()}/${multiPartFile.name}"
    nativeFetch(PureClientRequest(buildUrlString("file://file.std.dweb/write") {
      parameters.append("path", writePath)
      parameters.append("create", "true")
    }, PureMethod.POST, body = IPureBody.from(data)))

    val realPath = nativeFetch("file://file.std.dweb/realPath?path=${writePath}").text()
    return "file://$realPath"
  }

  override suspend fun _shutdown() {}

}