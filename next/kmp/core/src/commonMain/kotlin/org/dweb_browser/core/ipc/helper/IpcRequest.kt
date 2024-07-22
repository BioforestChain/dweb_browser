package org.dweb_browser.core.ipc.helper

import io.ktor.http.Url
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.helper.IFrom
import org.dweb_browser.helper.collectIn
import org.dweb_browser.pure.http.PureChannel
import org.dweb_browser.pure.http.PureFrame
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod


const val X_IPC_UPGRADE_KEY = "X-Dweb-Ipc-Upgrade-Key"
const val PURE_CHANNEL_EVENT_PREFIX = "§-"


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
) : IpcMessage, IFrom, RawAble<IpcReqMessage> {

  val uri by lazy { Url(url) }

  protected open val typeTag = "IpcRequest"
  override fun toString() = "$typeTag@$reqId/$method/$url".let { str ->
    if (ipc.debugIpc.isEnable) "$str{${
      headers.toList().joinToString(", ") { it.first + ":" + it.second }
    }}" + "" else str
  }

  companion object {}

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

  override val stringAble by lazy {
    IpcReqMessage(reqId, method, url, headers.toMap(), body.metaBody)
  }
}


/**
 * 一个将 pureChannel 与 ipc 进行关联转换的函数
 *
 * 目前这里使用一个 ipcEvent 来承载 pureChannel，二者转换几乎没有性能损失
 *
 * 请使用 launch(start = CoroutineStart.UNDISPATCHED) 启动这个函数，因为我们要尽量确保消息不丢失，而我们是依靠 orderBy 来消费 event，因此如果没有消费函数的阻塞，那么closing会自然而然地执行，
 */
@OptIn(DelicateCoroutinesApi::class, InternalAPI::class)
internal suspend fun pureChannelToIpcEvent(
  channelIpc: Ipc,
  pureChannelDeferred: Deferred<PureChannel>,
  debugTag: String,
) {
  val eventData = "${PURE_CHANNEL_EVENT_PREFIX}data"

  val ipcListenToChannelDeferred = CompletableDeferred<SendChannel<PureFrame>>()
  /// 这里需要立刻开始绑定事件，否则外部可能对ipc进行start，最终导致直接 ipc 关闭。那么这些 event 就会因为 close 而被强制消费
  val job = channelIpc.onEvent("IpcEventToPureChannel").collectIn(channelIpc.scope) { event ->
    val ipcEvent = event.consumeFilter { it.name == eventData } ?: return@collectIn
//    channelIpc.debugIpc(debugTag) { "inChannelData=$ipcEvent" }
    val ipcListenToChannel = ipcListenToChannelDeferred.await()
    if (!ipcListenToChannel.isClosedForSend) ipcListenToChannel.send(ipcEvent.toPureFrame())
  }
  channelIpc.launchJobs += job
  // 等待外部提供 pureChannel
  val pureChannel = pureChannelDeferred.await()
  /// 绑定 ipc => channel 的关闭
  channelIpc.onClosed {
    channelIpc.debugIpc(debugTag) { "ipc will-close outgoing-channel" }
    // 这里只是关闭输出
    pureChannel.closeOutgoing()
  }
  job.invokeOnCompletion {
    channelIpc.debugIpc(debugTag) { "ipc will-close channel" }
    // 这里做完全的关闭
    pureChannel.close()
  }
  val ctx = pureChannel.start()
  channelIpc.debugIpc(debugTag) { "pureChannel start" }
  /**收到ipcEvent时，需要对其进行接收的 channel*/
  ipcListenToChannelDeferred.complete(ctx.incomeChannel)
  /**收到pureFrame时，需要将其转发给ipc的 channel*/
  val channelForIpcPost = ctx.outgoingChannel

  channelIpc.start(reason = debugTag)
  /// 将PureFrame转成IpcEvent，然后一同发给对面
  for (pureFrame in channelForIpcPost) {
    val ipcDataEvent = IpcEvent.fromPureFrame(
      eventData, pureFrame,
      // 这里使用和生命周期一致的 order，以确保对面反能在数据消息接收完后再处理关闭信号
      orderBy = IpcLifecycle.DEFAULT_ORDER,
    )
//    channelIpc.debugIpc(debugTag) { "outChannelData=$ipcDataEvent" }
    channelIpc.postMessage(ipcDataEvent)
  }
  /// 绑定 channel => ipc 的关闭
  channelIpc.debugIpc(debugTag) { "channel will-close ipc" }
  channelIpc.close()
}

@Serializable
@SerialName(IPC_MESSAGE_TYPE_REQUEST)
data class IpcReqMessage(
  val reqId: Int,
  val method: PureMethod,
  val url: String,
  val headers: MutableMap<String, String>,
  val metaBody: MetaBody,
) : IpcRawMessage
