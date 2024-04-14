package org.dweb_browser.core.ipc.helper

import io.ktor.util.InternalAPI
import kotlinx.coroutines.launch
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.IpcRequestInit
import org.dweb_browser.helper.LateInit
import org.dweb_browser.helper.eprintln
import org.dweb_browser.helper.falseAlso
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureStream

class IpcClientRequest(
  reqId: Int,
  url: String,
  method: PureMethod,
  headers: PureHeaders,
  body: IpcBody,
  ipc: Ipc,
  override val from: Any? = null,
) : IpcRequest(
  reqId = reqId, url = url, method = method, headers = headers, body = body, ipc = ipc
) {
  companion object {

    fun fromText(
      reqId: Int,
      url: String,
      method: PureMethod = PureMethod.GET,
      headers: PureHeaders = PureHeaders(),
      text: String,
      ipc: Ipc,
    ) = IpcClientRequest(
      reqId,
      url,
      method,
      headers,// 这里 content-length 默认不写，因为这是要算二进制的长度，我们这里只有在字符串的长度，不是一个东西
      IpcBodySender.fromText(text, ipc),
      ipc,
    );

    fun fromBinary(
      reqId: Int,
      method: PureMethod,
      url: String,
      headers: PureHeaders = PureHeaders(),
      binary: ByteArray,
      ipc: Ipc,
    ) = IpcClientRequest(
      reqId,
      url,
      method,
      headers.also {
        headers.init("Content-Type", "application/octet-stream");
        headers.init("Content-Length", binary.size.toString());
      },
      IpcBodySender.fromBinary(binary, ipc),
      ipc,
    )

    suspend fun fromStream(
      reqId: Int,
      method: PureMethod,
      url: String,
      headers: PureHeaders = PureHeaders(),
      stream: PureStream,
      ipc: Ipc,
      size: Long? = null,
    ) = IpcClientRequest(
      reqId,
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

    suspend fun fromRequest(
      reqId: Int, ipc: Ipc, url: String, init: IpcRequestInit, from: Any? = null,
    ) = IpcClientRequest(
      reqId, url, init.method, init.headers, IpcBodySender.from(init.body, ipc), ipc, from
    )

  }

  internal val server = LateInit<IpcServerRequest>()
  fun toServer(serverIpc: Ipc) = server.getOrInitSync {
    IpcServerRequest(
      reqId = reqId,
      url = url,
      method = method,
      headers = headers,
      body = body,
      ipc = serverIpc,
      from = this,
    )
  }

  internal val pure = LateInit<PureClientRequest>()
}

@OptIn(InternalAPI::class)
suspend fun PureClientRequest.toIpc(
  reqId: Int,
  postIpc: Ipc,
): IpcClientRequest {
  val pureRequest = this
  if (pureRequest.hasChannel) {
    val debugTag = "IpcClient/channelToIpc"
    val channelIpc = postIpc.fork(autoStart = true, startReason = "PureClientRequestToIpc")
    val eventNameBase = "$PURE_CHANNEL_EVENT_PREFIX${channelIpc.pid}"

    postIpc.debugIpc(debugTag) { "forkedIpc:${channelIpc.pid}" }
    postIpc.scope.launch {
      val pureChannel = pureRequest.getChannel()
      /// 不论是请求者还是响应者
      /// 那么意味着数据需要通过ipc来进行发送。所以我需要将 pureChannel 中要发送的数据读取出来进行发送
      /// 反之，ipc收到的数据也要作为 pureChannel 的
      val channelContext = pureChannel.start()
      channelIpc.debugIpc(debugTag, "pureChannel start")

      pureChannelToIpcEvent(
        channelIpc,
        pureChannel = pureChannel,
        ipcListenToChannel = channelContext.incomeChannel,
        channelForIpcPost = channelContext.outgoingChannel,
        debugTag = debugTag
      )
    }

    val ipcRequest = IpcClientRequest.fromRequest(
      reqId,
      postIpc,
      pureRequest.href,
      IpcRequestInit(pureRequest.method, IPureBody.Empty, pureRequest.headers.copy().apply {
        init(X_IPC_UPGRADE_KEY, eventNameBase).falseAlso {
          eprintln("fromPure WARNING: SHOULD NOT HAPPENED, PURE_REQUEST CONTAINS 'X_IPC_UPGRADE_KEY' IN HEADERS")
        }
      }),
      from = this
    ).apply {
      pure.set(pureRequest)
    }

    return ipcRequest
  }
  return IpcClientRequest.fromRequest(
    reqId,
    postIpc,
    pureRequest.href,
    IpcRequestInit(pureRequest.method, pureRequest.body, pureRequest.headers)
  )
}