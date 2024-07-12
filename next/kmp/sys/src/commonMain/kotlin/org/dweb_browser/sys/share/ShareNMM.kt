package org.dweb_browser.sys.share

import io.ktor.http.ContentType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.ext.appendFile
import org.dweb_browser.core.std.file.ext.realPath
import org.dweb_browser.core.std.file.ext.writeFile
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.consumeEachCborPacket
import org.dweb_browser.helper.listen
import org.dweb_browser.helper.platform.MultiPartFile
import org.dweb_browser.helper.platform.MultiPartFileEncode
import org.dweb_browser.helper.platform.MultipartFieldData
import org.dweb_browser.helper.platform.MultipartFieldDescription
import org.dweb_browser.helper.platform.MultipartFieldEnd
import org.dweb_browser.helper.platform.MultipartFilePackage
import org.dweb_browser.helper.platform.MultipartFileType
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod

val debugShare = Debugger("Share")

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

  data class ShareChunkWriteTask(val writePath: String, val chunk: ByteArray)

  inner class ShareRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun _bootstrap() {
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
          val shareMM = getRemoteRuntime()
          val result = when {
            contentType.match(ContentType.MultiPart.FormData) -> try {
              val response = nativeFetch(
                PureClientRequest(
                  "file://multipart.http.std.dweb/parser",
                  PureMethod.POST,
                  request.headers,
                  request.body,
                )
              )
              val fieldWritePathMap = mutableMapOf</* field_index */Int, /* writePath */String>()
              val fileList = mutableListOf<String>()
              val channel = Channel<ShareChunkWriteTask>(capacity = Channel.RENDEZVOUS)
              val deferred = CompletableDeferred<Boolean>()
              scopeLaunch(cancelable = true) {
                for (task in channel) {
                  multipartFileDataAppendToTempFile(task.writePath, task.chunk)
                }
                deferred.complete(true)
              }
              response.body.toPureStream().getReader("share/form-data")
                .consumeEachCborPacket<MultipartFilePackage> { multipartFilePackage ->
                  when (multipartFilePackage.type) {
                    MultipartFileType.Desc -> {
                      val packet =
                        Cbor.decodeFromByteArray<MultipartFieldDescription>(multipartFilePackage.chunk)
                      fieldWritePathMap[packet.fieldIndex] =
                        "/cache/${randomUUID()}${packet.fileName?.let { "/$it" }}"
                    }

                    MultipartFileType.Data -> {
                      val packet =
                        Cbor.decodeFromByteArray<MultipartFieldData>(multipartFilePackage.chunk)
                      fieldWritePathMap[packet.fieldIndex]?.also { writePath ->
                        channel.send(ShareChunkWriteTask(writePath, packet.chunk))
                      }
                    }

                    MultipartFileType.End -> {
                      val packet =
                        Cbor.decodeFromByteArray<MultipartFieldEnd>(multipartFilePackage.chunk)
                      fieldWritePathMap[packet.fieldIndex]?.also { writePath ->
                        val realPath = realPath(writePath)
                        fileList.add("file://$realPath")
                      }
                    }

                    MultipartFileType.Close -> {
                      channel.close()
                    }
                  }
                }
              deferred.await()
              share(shareOptions, fileList, shareMM)
            } catch (e: Exception) {
              debugShare("ContentType/Form", "receiveMultipart error -> ${e.message}")
              share(shareOptions, null, shareMM)
            }

            contentType.match(ContentType.Application.Json) -> {
              val files = Json.decodeFromString<List<MultiPartFile>>(request.body.toPureString())
              val fileList = mutableListOf<String>()

              files.forEach {
                fileList.add(multipartFileDataWriteToTempFile(it))
              }

              share(shareOptions, fileList, shareMM)
            }

            contentType.match(ContentType.Application.Cbor) -> {
              val byteArray = request.body.toPureBinary()
              debugShare("ContentType/Cbor", "byteArray = ${byteArray.size}")

              val files = Cbor.decodeFromByteArray<List<MultiPartFile>>(byteArray)
              val fileList = mutableListOf<String>()

              files.forEach {
                fileList.add(multipartFileDataWriteToTempFile(it))
              }

              share(shareOptions, fileList, shareMM)
            }

            else -> {
              debugShare("share", "Unable to process $contentType")
              share(shareOptions, null, shareMM)
            }
          }

          debugShare("/share", "result => $result")
          ShareResult(result == "OK", result).toJsonElement()
        },
      ).cors()

      // 无法多次使用 onConnect 进行多次 listen，因此改用ipcConnectedProducer.consumer("for-share")
      ipcConnectedProducer.consumer("for-share").listen { connectEvent ->
        val (ipc) = connectEvent.consume()
        ipc.onEvent("shareLocalFile").collectIn(mmScope) { event ->
          event.consumeFilter { ipcEvent ->
            (ipcEvent.name == "shareLocalFile").trueAlso { // 用于文件分享，传入的内容是 《文件名&&文件路径》
              val filePath = ipcEvent.text
              val ret = share(
                ShareOptions(null, null, null), listOf("file://$filePath"), this@ShareRuntime
              )
              debugShare("shareLocalFile", ret)
              ipc.postMessage(IpcEvent.fromUtf8("shareLocalFile", ret))
            }
          }
        }
      }
    }


    private suspend fun multipartFileDataWriteToTempFile(
      multiPartFile: MultiPartFile,
    ): String {
      val writePath = "/cache/${randomUUID()}/${multiPartFile.name}"
      writeFile(
        writePath, body = IPureBody.from(
          multiPartFile.data, encoding = when (multiPartFile.encoding) {
            MultiPartFileEncode.UTF8 -> IPureBody.Companion.PureStringEncoding.Utf8
            MultiPartFileEncode.BASE64 -> IPureBody.Companion.PureStringEncoding.Base64
          }
        )
      )

      val realPath = realPath(writePath)
      return "file://$realPath"
    }

    private suspend fun multipartFileDataAppendToTempFile(writePath: String, chunk: ByteArray) {
      appendFile(writePath, IPureBody.Companion.from(chunk))
    }

    override suspend fun _shutdown() {}

  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = ShareRuntime(bootstrapContext)
}