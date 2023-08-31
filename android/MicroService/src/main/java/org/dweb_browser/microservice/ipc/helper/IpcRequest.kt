package org.dweb_browser.microservice.ipc.helper

import kotlinx.serialization.Serializable
import org.dweb_browser.microservice.help.InitRequest
import org.dweb_browser.microservice.help.buildRequestX
import org.dweb_browser.microservice.help.isWebSocket
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.IpcRequestInit
import org.http4k.core.Method
import org.http4k.core.Uri
import java.io.InputStream

class IpcRequest(
  val req_id: Int,
  val url: String,
  val method: IpcMethod,
  val headers: IpcHeaders,
  val body: IpcBody,
  val ipc: Ipc,
) : IpcMessage(IPC_MESSAGE_TYPE.REQUEST) {
  val uri by lazy { Uri.of(url) }

  init {
    if (body is IpcBodySender) {
      IpcBodySender.IPC.usableByIpc(ipc, body)
    }
  }

  companion object {

    fun fromText(
      req_id: Int,
      url: String,
      method: IpcMethod = IpcMethod.GET,
      headers: IpcHeaders = IpcHeaders(),
      text: String,
      ipc: Ipc
    ) = IpcRequest(
      req_id,
      url,
      method,
      headers,// 这里 content-length 默认不写，因为这是要算二进制的长度，我们这里只有在字符串的长度，不是一个东西
      IpcBodySender.fromText(text, ipc),
      ipc,
    );

    fun fromBinary(
      req_id: Int,
      method: IpcMethod,
      url: String,
      headers: IpcHeaders = IpcHeaders(),
      binary: ByteArray,
      ipc: Ipc
    ) = IpcRequest(
      req_id,
      url,
      method,
      headers.also {
        headers.init("Content-Type", "application/octet-stream");
        headers.init("Content-Length", binary.size.toString());
      },
      IpcBodySender.fromBinary(binary, ipc),
      ipc,
    )

    fun fromStream(
      req_id: Int,
      method: IpcMethod,
      url: String,
      headers: IpcHeaders = IpcHeaders(),
      stream: InputStream,
      ipc: Ipc,
      size: Long? = null
    ) = IpcRequest(
      req_id,
      url,
      method,
      headers.also {
        headers.init("Content-Type", "application/octet-stream");
        if (size !== null) {
          headers.init("Content-Length", size.toString());
        }
      },
      IpcBodySender.fromStream(stream, ipc),
      ipc,
    )

    fun fromRequest(
      req_id: Int, ipc: Ipc, url: String, init: IpcRequestInit
    ) = IpcRequest(
      req_id,
      url,
      IpcMethod.from(init.method),
      IpcHeaders(init.headers),
      if (isWebSocket(init.method, init.headers)) {
        IpcBodySender.fromStream(init.body.stream, ipc)
      } else if (init.method == Method.GET || init.method == Method.HEAD) {
        IpcBodySender.fromText("", ipc)
      } else when (init.body.length) {
        0L -> IpcBodySender.fromText("", ipc)
        null -> IpcBodySender.fromStream(init.body.stream, ipc)
        else -> IpcBodySender.fromBinary(init.body.payload.array(), ipc)
      },
      ipc,
    )
  }

  /**
   * 判断是否是双工协议
   *
   * 比如目前双工协议可以由 WebSocket 来提供支持
   */
  fun isDuplex(): Boolean {
    return isWebSocket(method.http4kMethod, headers.toList())
  }

  fun toRequest() = buildRequestX(url, InitRequest(method.http4kMethod, headers.toList(), body.raw))

  val ipcReqMessage by lazy {
    IpcReqMessage(req_id, method, url, headers.toMap(), body.metaBody)
  }

  override fun toString() = "#IpcRequest/$method/$url"
}

@Serializable
data class IpcReqMessage(
  val req_id: Int,
  val method: IpcMethod,
  val url: String,
  val headers: MutableMap<String, String>,
  val metaBody: MetaBody,
) : IpcMessage(IPC_MESSAGE_TYPE.REQUEST)
