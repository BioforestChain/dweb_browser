package org.dweb_browser.core.ipc.helper

import io.ktor.http.Url
import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.buildRequestX
import org.dweb_browser.core.help.isWebSocket
import org.dweb_browser.core.http.PureStream
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.IpcRequestInit
import org.dweb_browser.core.ipc.debugIpc

class IpcRequest(
  val req_id: Int,
  val url: String,
  val method: IpcMethod,
  val headers: IpcHeaders,
  val body: IpcBody,
  val ipc: Ipc,
) : IpcMessage(IPC_MESSAGE_TYPE.REQUEST) {
  val uri by lazy { Url(url) }

  init {
    if (body is IpcBodySender) {
      IpcBodySender.IPC.usableByIpc(ipc, body)
    }
  }

  override fun toString() = "IpcRequest@$req_id/$method/$url".let { str ->
    if (debugIpc.isEnable) "$str{${
      headers.toList().joinToString(", ") { it.first + ":" + it.second }
    }}" + "" else str
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
      stream: PureStream,
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
      init.method,
      init.headers,
      IpcBodySender.from(init.body, ipc),
      ipc,
    )
  }

  /**
   * 判断是否是双工协议
   *
   * 比如目前双工协议可以由 WebSocket 来提供支持
   */
  fun isDuplex(): Boolean {
    return isWebSocket(IpcMethod.from(method.ktorMethod), headers)
  }

  fun toRequest() = buildRequestX(url, method, headers, body.raw)

  val ipcReqMessage by lazy {
    IpcReqMessage(req_id, method, url, headers.toMap(), body.metaBody)
  }

}

@Serializable
data class IpcReqMessage(
  val req_id: Int,
  val method: IpcMethod,
  val url: String,
  val headers: MutableMap<String, String>,
  val metaBody: MetaBody,
) : IpcMessage(IPC_MESSAGE_TYPE.REQUEST)
