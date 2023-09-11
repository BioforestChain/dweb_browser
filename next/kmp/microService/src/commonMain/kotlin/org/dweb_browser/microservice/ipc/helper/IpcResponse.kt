package org.dweb_browser.microservice.ipc.helper

import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.core.ByteReadPacket
import kotlinx.serialization.Serializable
import org.dweb_browser.microservice.http.PureByteArrayBody
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.PureStreamBody
import org.dweb_browser.microservice.http.PureUtf8StringBody
import org.dweb_browser.microservice.ipc.Ipc

class IpcResponse(
  val req_id: Int,
  val statusCode: Int,
  val headers: IpcHeaders,
  val body: IpcBody,
  val ipc: Ipc,
) : IpcMessage(IPC_MESSAGE_TYPE.RESPONSE) {

  init {
    if (body is IpcBodySender) {
      IpcBodySender.IPC.usableByIpc(ipc, body)
    }
  }

  companion object {
    fun fromText(
      req_id: Int,
      statusCode: Int = 200,
      headers: IpcHeaders = IpcHeaders(),
      text: String,
      ipc: Ipc
    ) = IpcResponse(
      req_id,
      statusCode,
      headers.also { headers.init("Content-Type", "text/plain") },
      IpcBodySender.fromText(text, ipc),
      ipc,
    )

    fun fromBinary(
      req_id: Int, statusCode: Int = 200, headers: IpcHeaders, binary: ByteArray, ipc: Ipc
    ) = IpcResponse(
      req_id,
      statusCode,
      headers.also {
        headers.init("Content-Type", "application/octet-stream");
        headers.init("Content-Length", binary.size.toString());
      },
      IpcBodySender.fromBinary(binary, ipc),
      ipc,
    );


    fun fromStream(
      req_id: Int,
      statusCode: Int = 200,
      headers: IpcHeaders = IpcHeaders(),
      stream: ByteReadPacket,
      ipc: Ipc
    ) = IpcResponse(
      req_id,
      statusCode,
      headers.also {
        headers.init("Content-Type", "application/octet-stream");
      },
      IpcBodySender.fromStream(stream, ipc),
      ipc,
    )

    enum class BodyStrategy {
      AUTO,
      STREAM,
      BINARY,
    }

    fun fromResponse(
      req_id: Int, response: PureResponse, ipc: Ipc, bodyStrategy: BodyStrategy = BodyStrategy.AUTO
    ) = IpcResponse(
      req_id,
      response.statusCode.value,
      response.headers,
      when (val len = response.body.contentLength) {
        0L -> IpcBodySender.fromText("", ipc)
        else -> when (bodyStrategy) {
          BodyStrategy.AUTO -> if (len == null) false else len <= DEFAULT_BUFFER_SIZE
          BodyStrategy.STREAM -> false
          BodyStrategy.BINARY -> true
        }.let { asBinary ->
          if (asBinary) {
            IpcBodySender.fromBinary(response.body.toByteArray(), ipc)
          } else {
            IpcBodySender.fromStream(response.body.toStream(), ipc)
          }
        }
      },
      ipc,
    )
  }

  fun toResponse() =
    PureResponse(
      HttpStatusCode.fromValue(statusCode), this.headers, body = when (val body = body.raw) {
        is String -> PureUtf8StringBody(body)
        is ByteArray -> PureByteArrayBody(body)
        is ByteChannel -> PureStreamBody(body)
        else -> throw Exception("invalid body to response: $body")
      }
    )

  val ipcResMessage by lazy {
    IpcResMessage(req_id, statusCode, headers.toMap(), body.metaBody)
  }
}

@Serializable
data class IpcResMessage(
  val req_id: Int,
  val statusCode: Int,
  val headers: MutableMap<String, String>,
  val metaBody: MetaBody,
) : IpcMessage(IPC_MESSAGE_TYPE.RESPONSE)
