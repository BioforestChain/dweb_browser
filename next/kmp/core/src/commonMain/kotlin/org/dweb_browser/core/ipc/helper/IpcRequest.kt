package org.dweb_browser.core.ipc.helper

import io.ktor.http.Url
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.buildRequestX
import org.dweb_browser.core.http.IPureBody
import org.dweb_browser.core.http.PureChannel
import org.dweb_browser.core.http.PureClientRequest
import org.dweb_browser.core.http.PureFrame
import org.dweb_browser.core.http.PureServerRequest
import org.dweb_browser.core.http.PureStream
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.IpcRequestInit
import org.dweb_browser.core.ipc.debugIpc
import org.dweb_browser.helper.IFrom
import org.dweb_browser.helper.LateInit
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.eprintln
import org.dweb_browser.helper.falseAlso
import kotlin.coroutines.coroutineContext


const val PURE_CHANNEL_EVENT_PREFIX = "§"
const val X_IPC_UPGRADE_KEY = "X-Dweb-Ipc-Upgrade-Key"

class IpcClientRequest(
  req_id: Int,
  url: String,
  method: IpcMethod,
  headers: IpcHeaders,
  body: IpcBody,
  ipc: Ipc,
  override val from: Any? = null
) : IpcRequest(
  req_id = req_id,
  url = url,
  method = method,
  headers = headers,
  body = body,
  ipc = ipc
) {
  companion object {

    fun fromText(
      req_id: Int,
      url: String,
      method: IpcMethod = IpcMethod.GET,
      headers: IpcHeaders = IpcHeaders(),
      text: String,
      ipc: Ipc
    ) = IpcClientRequest(
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
    ) = IpcClientRequest(
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
    ) = IpcClientRequest(
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
      req_id: Int, ipc: Ipc, url: String, init: IpcRequestInit, from: Any? = null
    ) = IpcClientRequest(
      req_id,
      url,
      init.method,
      init.headers,
      IpcBodySender.from(init.body, ipc),
      ipc,
      from
    )

    suspend fun PureClientRequest.toIpc(
      req_id: Int,
      postIpc: Ipc,
    ): IpcClientRequest {
      val pureRequest = this
      if (pureRequest.hasChannel) {
        val eventNameBase =
          "$PURE_CHANNEL_EVENT_PREFIX-${postIpc.uid}/${req_id}/${duplexAcc.inc().value}"

        debugIpc("toIpc/client/hasChannel") { "create ipcEventBaseName:$eventNameBase => request:$pureRequest" }
        CoroutineScope(coroutineContext).launch {
          val pureChannel = pureRequest.getChannel()
          debugIpc("toIpc/client/channelToIpc") { "channelId:$eventNameBase => pureChannel:$pureChannel start!!" }

          /// 不论是请求者还是响应者
          /// 那么意味着数据需要通过ipc来进行发送。所以我需要将 pureChannel 中要发送的数据读取出来进行发送
          /// 反之，ipc收到的数据也要作为 pureChannel 的
          val channelContext = pureChannel.start()
          pureChannelToIpcEvent(
            eventNameBase,
            postIpc,
            pureChannel,
            channelByIpcEmit = channelContext.income,
            channelForIpcPost = channelContext.outgoing,
            _debugTag = "toIpc/client/channelToIpc"
          ) { }
        }

        val ipcRequest = fromRequest(
          req_id, postIpc, pureRequest.href,
          IpcRequestInit(
            pureRequest.method,
            IPureBody.Empty,
            pureRequest.headers.copy().apply {
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
        req_id, postIpc, pureRequest.href,
        IpcRequestInit(pureRequest.method, pureRequest.body, pureRequest.headers)
      )
    }
  }

  internal val server = LateInit<IpcServerRequest>()
  fun toServer(serverIpc: Ipc) = server.getOrInit {
    IpcServerRequest(
      req_id = req_id,
      url = url,
      method = method,
      headers = headers,
      body = body,
      ipc = serverIpc,
      from = this,
    )
  }

  internal val pure = LateInit<PureClientRequest>()
//
//  suspend fun toPure() = pure.getOrInit {
//    buildRequestX(url, method, headers, body.raw, from = this).let { pureRequest ->
//      if (hasDuplex) {
//        debugIpc(
//          "toPure/client/ipcToChannel",
//          "channelId:$duplexEventBaseName => request:$this start!!"
//        )
//
//        val income = Channel<PureFrame>()
//        val outgoing = Channel<PureFrame>()
//        val pureChannel = PureChannel(income, outgoing, from = this)
//        CoroutineScope(coroutineContext).launch {
//          pureChannelToIpcEvent(
//            duplexEventBaseName!!,
//            ipc,
//            pureChannel,
//            channelByIpcEmit = income,
//            channelForIpcPost = outgoing,
//            _debugTag = "toPure/client/ipcToChannel",
//          ) { pureChannel.afterStart(); }
//        }
//
//        pureRequest.copy(
//          channel = CompletableDeferred(),
//          headers = pureRequest.headers.copy().apply { delete(X_IPC_UPGRADE_KEY) })
//          .apply { completeChannel(pureChannel) }
//      } else pureRequest
//    }
//  }
}


class IpcServerRequest(
  req_id: Int,
  url: String,
  method: IpcMethod,
  headers: IpcHeaders,
  body: IpcBody,
  ipc: Ipc,
  override val from: Any? = null
) :
  IpcRequest(
    req_id = req_id,
    url = url,
    method = method,
    headers = headers,
    body = body,
    ipc = ipc
  ) {

  fun getClient() = findFrom { if (it is IpcClientRequest) it else null }

  internal val pure = LateInit<PureServerRequest>()

  suspend fun toPure() = pure.getOrInit {
    buildRequestX(url, method, headers, body.raw, from = this).let { pureRequest ->
      /// 如果存在双工通道，那么这个 pureRequest 用不了，需要重新构建一个新的 PureServerRequest
      if (hasDuplex) {
        val eventNameBase = duplexEventBaseName!!
        debugIpc(
          "toPure/server/ipcToChannel",
          "channelId:$eventNameBase => request:$this start!!"
        )

        val pureChannelDeferred = CompletableDeferred<PureChannel>()
        CoroutineScope(coroutineContext).launch {
          val pureChannel = pureChannelDeferred.await();
          val ctx = pureChannel.start()
          pureChannelToIpcEvent(
            eventNameBase,
            ipc,
            pureChannel,
            channelByIpcEmit = ctx.income,
            channelForIpcPost = ctx.outgoing,
            _debugTag = "toPure/server/ipcToChannel",
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
  val req_id: Int,
  val url: String,
  val method: IpcMethod,
  val headers: IpcHeaders,
  val body: IpcBody,
  val ipc: Ipc,
) : IpcMessage(IPC_MESSAGE_TYPE.REQUEST), IFrom {


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

    internal val duplexAcc by lazy { SafeInt(0) }

    /**
     * 一个将 pureChannel 与 ipc 进行关联转换的函数
     *
     * TODO 这里应该使用 fork():Ipc 来承载 pureChannel
     * 目前这里使用一个 ipcEvent 来承载 pureChannel，行为上比较奇怪，性能上也不是最佳
     */
    internal suspend fun pureChannelToIpcEvent(
      eventNameBase: String,
      ipc: Ipc,
      pureChannel: PureChannel,
      /**收到ipcEvent时，需要对其进行接收的 channel*/
      channelByIpcEmit: SendChannel<PureFrame>,
      /**收到pureFrame时，需要将其转发给ipc的 channel*/
      channelForIpcPost: ReceiveChannel<PureFrame>,
      _debugTag: String = "pureChannelToIpcEvent",
      waitReadyToStart: suspend () -> Unit,
    ) {
      val eventStart = "$eventNameBase/start"
      val eventData = "$eventNameBase/data"
      val eventClose = "$eventNameBase/close"
      val started = CompletableDeferred<IpcEvent>()
      /// 将收到的IpcEvent转成PureFrame
      val off = ipc.onEvent { (ipcEvent) ->
        when (ipcEvent.name) {
          eventData -> {
            debugIpc(_debugTag) { "$ipc onIpcEventData:$ipcEvent $pureChannel" }
            channelByIpcEmit.send(ipcEvent.toPureFrame())
          }

          eventStart -> {
            debugIpc(_debugTag) { "$ipc onIpcEventStart:$ipcEvent $pureChannel" }
            started.complete(ipcEvent)
          }

          eventClose -> {
            debugIpc(_debugTag) { "$ipc onIpcEventClose:$ipcEvent $pureChannel" }
            pureChannel.close()
          }
        }
      }
      debugIpc(_debugTag) { "waitLocaleStart:$eventNameBase $pureChannel" }
      // 提供回调函数，等待外部调用者执行开始指令
      waitReadyToStart()
      debugIpc(_debugTag) { "waitRemoteStart:$eventNameBase $pureChannel" }
      // 首先自己发送start，告知对方自己已经准备好数据接收了
      ipc.postMessage(IpcEvent.fromUtf8(eventStart, ""))
      // 同时也要等待对方发送 start 信号过来，那么也将 start 回传，避免对方遗漏前面的 start 消息
      val ipcStartEvent = started.await()
      debugIpc(_debugTag) { "$ipc postIpcEventStart:$ipcStartEvent $pureChannel" }
      ipc.postMessage(ipcStartEvent)
      /// 将PureFrame转成IpcEvent，然后一同发给对面
      for (pureFrame in channelForIpcPost) {
        val ipcDataEvent = IpcEvent.fromPureFrame(eventData, pureFrame)
        debugIpc(_debugTag) { "$ipc postIpcEventData:$ipcDataEvent $pureChannel" }
        ipc.postMessage(ipcDataEvent)
      }
      // 关闭的时候，发一个信号给对面
      val ipcCloseEvent = IpcEvent.fromUtf8(eventClose, "")
      debugIpc(_debugTag) { "$ipc postIpcEventClose:$ipcCloseEvent $pureChannel" }
      ipc.postMessage(ipcCloseEvent)
      off() // 移除事件监听
    }
  }

  /**
   * 判断是否是双工协议
   *
   * 注意，这里WebSocket，但是可以由 WebSocket 来提供支持
   * WebSocket 是在头部中有 Upgrade ，我们这里是 X_IPC_PURE_CHANNEL_ID
   */
  val hasDuplex get() = duplexEventBaseName != null
  protected val duplexEventBaseName by lazy {
    var eventNameBase: String? = null
    headers.get(X_IPC_UPGRADE_KEY)?.also {
      if (it.startsWith(PURE_CHANNEL_EVENT_PREFIX)) {
        eventNameBase = it
      }
    }
    eventNameBase
  }

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
