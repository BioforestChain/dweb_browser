package org.dweb_browser.core.ipc.helper

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.pure.http.DEFAULT_BUFFER_SIZE
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStream


class IpcResponse(
  val reqId: Int,
  val statusCode: Int,
  val headers: PureHeaders,
  val body: IpcBody,
  val ipc: Ipc,
) : IpcMessage, RawAble<IpcResMessage> {
  override fun toString() = "IpcResponse@$reqId/[$statusCode]".let { str ->
    if (ipc.debugIpc.isEnable) "$str{${
      headers.toList().joinToString(", ") { it.first + ":" + it.second }
    }}" + "" else str
  }

  companion object {
    fun fromText(
      reqId: Int,
      statusCode: Int = 200,
      headers: PureHeaders = PureHeaders(),
      text: String,
      ipc: Ipc,
    ) = IpcResponse(
      reqId,
      statusCode,
      headers.also { headers.init("Content-Type", "text/plain") },
      IpcBodySender.fromText(text, ipc),
      ipc,
    )

    fun fromBinary(
      reqId: Int, statusCode: Int = 200, headers: PureHeaders, binary: ByteArray, ipc: Ipc,
    ) = IpcResponse(
      reqId,
      statusCode,
      headers.also {
        headers.init("Content-Type", "application/octet-stream");
        headers.init("Content-Length", binary.size.toString());
      },
      IpcBodySender.fromBinary(binary, ipc),
      ipc,
    );


    suspend fun fromStream(
      reqId: Int,
      statusCode: Int = 200,
      headers: PureHeaders = PureHeaders(),
      stream: PureStream,
      ipc: Ipc,
    ) = IpcResponse(
      reqId,
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
      reqId: Int, response: PureResponse, ipc: Ipc, bodyStrategy: BodyStrategy = BodyStrategy.AUTO,
    ) = IpcResponse(
      reqId,
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

  fun toPure() = PureResponse(HttpStatusCode.fromValue(statusCode), this.headers, body = body.raw)

  override val stringAble by lazy {
    IpcResMessage(reqId, statusCode, headers.toMap(), body.metaBody)
  }
}

@Serializable
@SerialName(IPC_MESSAGE_TYPE_RESPONSE)
data class IpcResMessage(
  val reqId: Int,
  val statusCode: Int,
  val headers: MutableMap<String, String>,
  val metaBody: MetaBody,
) : IpcRawMessage
