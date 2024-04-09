package org.dweb_browser.core.ipc.helper

import io.ktor.http.Url
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.IpcRequestInit
import org.dweb_browser.helper.IFrom
import org.dweb_browser.helper.LateInit
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.eprintln
import org.dweb_browser.helper.falseAlso
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureChannel
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureCloseFrame
import org.dweb_browser.pure.http.PureFrame
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureServerRequest
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.pure.http.buildRequestX


const val X_IPC_UPGRADE_KEY = "X-Dweb-Ipc-Upgrade-Key"
const val PURE_CHANNEL_EVENT_PREFIX = "§-"

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

    suspend fun PureClientRequest.toIpc(
      reqId: Int,
      postIpc: Ipc,
    ): IpcClientRequest {
      val pureRequest = this
      if (pureRequest.hasChannel) {
        val channelIpc = postIpc.fork(autoStart = true, startReason = "PureClientRequestToIpc")
        val eventNameBase = "$PURE_CHANNEL_EVENT_PREFIX${channelIpc.pid}"

        postIpc.debugIpc("ipcClient/hasChannel") { "create ipcEventBaseName:$eventNameBase => request:$pureRequest" }
        postIpc.scope.launch {
          val pureChannel = pureRequest.getChannel()
//          debugIpc("ipcClient/channelToIpc") { "channelId:$eventNameBase => pureChannel:$pureChannel start!!" }
          /// 不论是请求者还是响应者
          /// 那么意味着数据需要通过ipc来进行发送。所以我需要将 pureChannel 中要发送的数据读取出来进行发送
          /// 反之，ipc收到的数据也要作为 pureChannel 的
          val channelContext = pureChannel.start()

          pureChannelToIpcEvent(
            channelIpc,
            pureChannel,
            channelByIpcEmit = channelContext.income,
            channelForIpcPost = channelContext.outgoing,
            debugTag = "IpcClient/channelToIpc"
          ) { }
        }

        val ipcRequest = fromRequest(
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
      return fromRequest(
        reqId,
        postIpc,
        pureRequest.href,
        IpcRequestInit(pureRequest.method, pureRequest.body, pureRequest.headers)
      )
    }
  }

  internal val server = LateInit<IpcServerRequest>()
  fun toServer(serverIpc: Ipc) = server.getOrInit {
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


class IpcServerRequest(
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

  fun getClient() = findFrom { if (it is IpcClientRequest) it else null }

  internal val pure = LateInit<PureServerRequest>()

  suspend fun toPure() = pure.getOrInit {
    buildRequestX(url, method, headers, body.raw, from = this).let { pureRequest ->
      /// 如果存在双工通道，那么这个 pureRequest 用不了，需要重新构建一个新的 PureServerRequest
      if (hasDuplex) {
        val forkedIpcId = duplexIpcId!!
        ipc.debugIpc(
          "PureServer/ipcToChannel",
          "forkedIpcId:$forkedIpcId => request:$this start!!"
        )
        val channelIpc = ipc.waitForkedIpc(forkedIpcId)

        val pureChannelDeferred = CompletableDeferred<PureChannel>()
        ipc.scope.launch {
          val pureChannel = pureChannelDeferred.await();
          val ctx = pureChannel.start()
          pureChannelToIpcEvent(
            channelIpc,
            pureChannel,
            channelByIpcEmit = ctx.income,
            channelForIpcPost = ctx.outgoing,
            debugTag = "PureServer/ipcToChannel",
          ) { }
        }

        PureServerRequest(
          href = pureRequest.href,
          method = pureRequest.method,
          headers = headers.copy().apply { delete(X_IPC_UPGRADE_KEY) },
          body = pureRequest.body,
          channel = pureChannelDeferred,
          from = pureRequest.from,
        ).also { pureServerRequest ->
          pureChannelDeferred.complete(PureChannel(pureServerRequest))
        }
      } else pureRequest.toServer()
    }
  }
}

/**
 *
 * 不论是 IpcRequest 还是 PureRequest，它们本身存在两种可能：
 * 1. 一种是在请求的发起者那侧，所以本质上是 IpcClientRequest 与 PureClientRequest
 * 1. 一种是请求的响应者那侧，所以本质上是 IpcServerRequest 与 PureServerRequest
 *
 * 一般情况下，Client与Server没什么区别，但因为 Request 本身可能携带 Socket（PureChannel）
 * > 这里的 Client 指 IpcClientRequest 或者 PureClientRequest，本质上没有太多区别，Pure 系列属于更高级别的抽象，可以代表 HttpRequest、IpcRequest 等各种 Request；Server 同理。
 *
 * 因此这里头就存在通讯反向的问题:
 * 1. 如果是 Client<=>Client 或者 Server<=>Server 的转换，那么这个方向不需要任何变化
 * 1. 反之，如果是 Client<=>Server 或者 Server<=>Client，那么这里本身就需要一层转换:
 *    > 就是将 Client.outgoing <=> Server.income && Client.income <=> Server.outgoing
 *
 */
sealed class IpcRequest(
  val reqId: Int,
  val url: String,
  val method: PureMethod,
  val headers: PureHeaders,
  val body: IpcBody,
  val ipc: Ipc,
) : IpcMessage(IPC_MESSAGE_TYPE.REQUEST), IFrom {

  val uri by lazy { Url(url) }

  override fun toString() = "IpcRequest@$reqId/$method/$url".let { str ->
    if (ipc.debugIpc.isEnable) "$str{${
      headers.toList().joinToString(", ") { it.first + ":" + it.second }
    }}" + "" else str
  }

  companion object {

    internal val duplexAcc by lazy { SafeInt(0) }

    /**
     * 一个将 pureChannel 与 ipc 进行关联转换的函数
     *
     * TODO 这里应该使用 fork():Ipc 来承载 pureChannel
     * 目前这里使用一个 ipcEvent 来承载 pureChannel，行为上比较奇怪，性能上也不是最佳
     */
    @OptIn(DelicateCoroutinesApi::class)
    internal suspend fun pureChannelToIpcEvent(
      channelIpc: Ipc,
      pureChannel: PureChannel,
      /**收到ipcEvent时，需要对其进行接收的 channel*/
      channelByIpcEmit: SendChannel<PureFrame>,
      /**收到pureFrame时，需要将其转发给ipc的 channel*/
      channelForIpcPost: ReceiveChannel<PureFrame>,
      debugTag: String = "pureChannelToIpcEvent",
      waitReadyToStart: suspend () -> Unit,
    ) {
      val eventStart = "start"
      val eventData = "data"
      val eventClose = "close"
      val started = CompletableDeferred<IpcEvent>()
      coroutineScope {
        channelIpc.onEvent("pureChannelToIpcEvent").collectIn(this) { event ->
          val ipcEvent = event.data
          when (ipcEvent.name) {
            eventData -> {
//            debugIpc(_debugTag) { "$ipc onIpcEventData:$ipcEvent" }
              if (!channelByIpcEmit.isClosedForSend) channelByIpcEmit.send(ipcEvent.toPureFrame())
            }

            eventStart -> {
              channelIpc.debugIpc(debugTag) { "$channelIpc onIpcEventStart:$ipcEvent" }
              started.complete(ipcEvent)
            }

            eventClose -> {
              channelIpc.debugIpc(debugTag) { "$channelIpc onIpcEventClose:$ipcEvent " }
              pureChannel.close()
            }

            else -> return@collectIn
          }
          event.consume()
        }
        channelIpc.debugIpc(debugTag) { "waitLocaleStart ${channelIpc.debugId}" }
        // 提供回调函数，等待外部调用者执行开始指令
        waitReadyToStart()
        channelIpc.debugIpc(debugTag) { "waitRemoteStart ${channelIpc.debugId}" }
        // 首先自己发送start，告知对方自己已经准备好数据接收了
        channelIpc.postMessage(IpcEvent.fromUtf8(eventStart, ""))
        // 同时也要等待对方发送 start 信号过来，那么也将 start 回传，避免对方遗漏前面的 start 消息
        val ipcStartEvent = started.await()
        channelIpc.debugIpc(debugTag) { "$channelIpc postIpcEventStart:$ipcStartEvent ${channelIpc.debugId}" }
        channelIpc.postMessage(ipcStartEvent)
        /// 将PureFrame转成IpcEvent，然后一同发给对面
        for (pureFrame in channelForIpcPost) {
          when (pureFrame) {
            PureCloseFrame -> break;
            else -> {
              val ipcDataEvent = IpcEvent.fromPureFrame(eventData, pureFrame)
              channelIpc.debugIpc(debugTag) { "$channelIpc postIpcEventData:$ipcDataEvent ${channelIpc.debugId}" }
              channelIpc.postMessage(ipcDataEvent)
            }
          }
        }
        // 关闭的时候，发一个信号给对面
        val ipcCloseEvent = IpcEvent.fromUtf8(eventClose, "")
        channelIpc.debugIpc(debugTag) { "$channelIpc postIpcEventClose:$ipcCloseEvent ${channelIpc.debugId}" }
        channelIpc.postMessage(ipcCloseEvent)
      }
    }
  }

  /**
   * 判断是否是双工协议
   *
   * 注意，这里WebSocket，但是可以由 WebSocket 来提供支持
   * WebSocket 是在头部中有 Upgrade ，我们这里是 X_IPC_PURE_CHANNEL_ID
   */
  val hasDuplex get() = duplexIpcId != null
  protected val duplexIpcId by lazy {
    var forkedIpcId: Int? = null
    headers.get(X_IPC_UPGRADE_KEY)?.also {
      if (it.startsWith(PURE_CHANNEL_EVENT_PREFIX)) {
        forkedIpcId = it.substring(PURE_CHANNEL_EVENT_PREFIX.length).toInt()
      }
    }
    forkedIpcId
  }

  val ipcReqMessage by lazy {
    IpcReqMessage(reqId, method, url, headers.toMap(), body.metaBody)
  }

}

@Serializable
data class IpcReqMessage(
  val reqId: Int,
  val method: PureMethod,
  val url: String,
  val headers: MutableMap<String, String>,
  val metaBody: MetaBody,
) : IpcMessage(IPC_MESSAGE_TYPE.REQUEST)
