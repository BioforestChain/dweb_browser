package org.dweb_browser.sys.media

import io.ktor.http.ContentType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.consumeEachCborPacket
import org.dweb_browser.helper.platform.MultiPartFile
import org.dweb_browser.helper.platform.MultipartFieldData
import org.dweb_browser.helper.platform.MultipartFieldDescription
import org.dweb_browser.helper.platform.MultipartFieldEnd
import org.dweb_browser.helper.platform.MultipartFilePackage
import org.dweb_browser.helper.platform.MultipartFileType
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod

val debugMedia = Debugger("Media")

class MediaNMM : NativeMicroModule("media.file.sys.dweb", "system media") {
  init {
    short_name = "Media";
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service)
    dweb_protocols = listOf("media.sys.dweb")
    /*dweb_permissions = listOf(
      DwebPermission(
        pid = "$mmid/savePictures",
        routes = listOf("file://$mmid/savePictures"),
        title = "存储到相册",
      )
    )*/
  }

  data class FieldChunkTask(val field_index: Int, val chunk: ByteArray)

  inner class MediaRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun _bootstrap() {
      routes(
        /** 保存图片到相册*/
        "/savePictures" bind PureMethod.POST by defineEmptyResponse {
          val contentType =
            request.headers.get("Content-Type")?.let { ContentType.parse(it) } ?: ContentType.Any
          val saveLocation = request.queryOrNull("saveLocation") ?: "DwebBrowser"
          debugMedia("savePictures", "contentType=$contentType saveLocation=$saveLocation")

          when {
            contentType.match(ContentType.MultiPart.FormData) -> {
              val response = nativeFetch(
                PureClientRequest(
                  href = "file://multipart.http.std.dweb/parser",
                  method = PureMethod.POST,
                  headers = request.headers,
                  body = request.body
                )
              )

              val fieldMediaMap = mutableMapOf</* field_index */Int, MediaPicture>()
              val channel = Channel<FieldChunkTask>(capacity = Channel.RENDEZVOUS)
              val deferred = CompletableDeferred<Boolean>()
              scopeLaunch(cancelable = true) {
                for (task in channel) {
                  fieldMediaMap[task.field_index]?.also {
                    it.consumePictureChunk(task.chunk)
                  }
                }
                deferred.complete(true)
              }
              response.body.toPureStream().getReader("savePictures")
                .consumeEachCborPacket<MultipartFilePackage> { multipartFilePackage ->
                  when (multipartFilePackage.type) {
                    MultipartFileType.Desc -> {
                      val packet =
                        Cbor.decodeFromByteArray<MultipartFieldDescription>(multipartFilePackage.chunk)
                      fieldMediaMap[packet.fieldIndex] = MediaPicture.create(saveLocation, packet)
                    }

                    MultipartFileType.Data -> {
                      val packet =
                        Cbor.decodeFromByteArray<MultipartFieldData>(multipartFilePackage.chunk)
                      fieldMediaMap[packet.fieldIndex]?.also {
                        channel.send(FieldChunkTask(packet.fieldIndex, packet.chunk))
                      }
                    }

                    MultipartFileType.End -> {
                      val packet =
                        Cbor.decodeFromByteArray<MultipartFieldEnd>(multipartFilePackage.chunk)
                      fieldMediaMap[packet.fieldIndex]?.also {
                        it.savePicture()
                      }
                    }

                    MultipartFileType.Close -> {
                      channel.close()
                    }
                  }
                }
            }

            contentType.match(ContentType.Application.Json) -> {
              val files = Json.decodeFromString<List<MultiPartFile>>(request.body.toPureString())
              // 目前只支持保存一个文件
              savePictures(saveLocation, files)
            }

            contentType.match(ContentType.Application.Cbor) -> {
              val files = Cbor.decodeFromByteArray<List<MultiPartFile>>(request.body.toPureBinary())
              // 目前只支持保存一个文件
              savePictures(saveLocation, files)
            }
          }
        }
      )
    }

    override suspend fun _shutdown() {
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = MediaRuntime(bootstrapContext)
}
