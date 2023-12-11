package org.dweb_browser.core.ipc.helper

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.PureStream
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.debugIpc


class IpcResponse(
  val req_id: Int,
  val statusCode: Int,
  val headers: IpcHeaders,
  val body: IpcBody,
  val ipc: Ipc,
) : IpcMessage(IPC_MESSAGE_TYPE.RESPONSE) {
  override fun toString() = "IpcResponse@$req_id/[$statusCode]".let { str ->
    if (debugIpc.isEnable) "$str{${
      headers.toList().joinToString(", ") { it.first + ":" + it.second }
    }}" + "" else str
  }

  init {
    if (body is IpcBodySender) {
      IpcBodySender.IPC.usableByIpc(ipc, body)
    }
  }

  companion object {
    fun fromText(
      req_id: Int, statusCode: Int = 200, headers: IpcHeaders = IpcHeaders(), text: String, ipc: Ipc
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
      stream: PureStream,
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
      AUTO, STREAM, BINARY,
    }

    suspend fun fromResponse(
      req_id: Int, response: PureResponse, ipc: Ipc, bodyStrategy: BodyStrategy = BodyStrategy.AUTO
    ) = IpcResponse(
      req_id,
      response.status.value,
      response.headers,
      when (val len = response.body.contentLength) {
        0L -> IpcBodySender.fromText("", ipc)
        else -> when (bodyStrategy) {
          BodyStrategy.AUTO -> if (len == null) false else len <= DEFAULT_BUFFER_SIZE
          BodyStrategy.STREAM -> false
          BodyStrategy.BINARY -> true
        }.let { asBinary ->
          if (asBinary) {
            IpcBodySender.fromBinary(response.body.toPureBinary(), ipc)
          } else {
            IpcBodySender.fromStream(response.body.toPureStream(), ipc)
          }
        }
      },
      ipc,
    )
  }

  fun toPure() =
    PureResponse(HttpStatusCode.fromValue(statusCode), this.headers, body = body.raw)

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
