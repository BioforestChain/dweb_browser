package org.dweb_browser.sys.share

import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.IPureBody
import org.dweb_browser.core.http.PureClientRequest
import org.dweb_browser.core.http.receiveMultipart
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.buildUrlString
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

  @Serializable
  enum class MultiPartFileEncode {
    @SerialName("utf8")
    UTF8,
    @SerialName("base64")
    BASE64
  }

  @Serializable
  data class MultiPartFile(
    val name: String,
    val size: Long,
    val type: String,
    val data: String,
    val encode: MultiPartFileEncode = MultiPartFileEncode.UTF8
  )

  @OptIn(ExperimentalSerializationApi::class)
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      /** 分享*/
      "/share" bind HttpMethod.Post by defineJsonResponse {
        val contentType =
          request.headers.get("Content-Type")?.let { ContentType.parse(it) } ?: ContentType.Any

        val shareOptions = ShareOptions(
          title = request.queryOrNull("title"),
          text = request.queryOrNull("text"),
          url = request.queryOrNull("url"),
        )
        val result = when {
          contentType.match(ContentType.MultiPart.FormData) ->
            try {
              share(shareOptions, request.receiveMultipart(), this@ShareNMM)
            } catch (e: Exception) {
              debugShare("/share", "receiveMultipart error -> ${e.message}")
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
            println("byteArray = ${byteArray.size}")

            val files = Cbor.decodeFromByteArray<List<MultiPartFile>>(byteArray)
            val fileList = mutableListOf<String>()

            files.forEach {
              fileList.add(multipartFileDataWriteToTempFile(it, ipc.remote.mmid))
            }

            share(shareOptions, fileList, this@ShareNMM)
          }

          else -> ""
        }

        debugShare("/share", "result => $result")
        return@defineJsonResponse ShareResult(result == "OK", result).toJsonElement()
      },
    ).cors()
  }

  private suspend fun multipartFileDataWriteToTempFile(multiPartFile: MultiPartFile, mmid: MMID): String {
    val data = when (multiPartFile.encode) {
      ShareNMM.MultiPartFileEncode.UTF8 -> multiPartFile.data.toUtf8ByteArray()
      ShareNMM.MultiPartFileEncode.BASE64 -> multiPartFile.data.toBase64ByteArray()
    }

    val writePath = "/cache/${mmid}/${randomUUID()}/${multiPartFile.name}"
    nativeFetch(PureClientRequest(buildUrlString("file://file.std.dweb/write") {
      parameters.append("path", writePath)
      parameters.append("create", "true")
    }, IpcMethod.POST, body = IPureBody.from(data)))

    val realPath = nativeFetch("file://file.std.dweb/realPath?path=${writePath}").text()
    return "file://$realPath"
  }

  override suspend fun _shutdown() {}

}