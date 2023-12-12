package org.dweb_browser.core.ipc.helper

import io.ktor.http.Url
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.buildRequestX
import org.dweb_browser.core.http.IPureBody
import org.dweb_browser.core.http.PureChannel
import org.dweb_browser.core.http.PureClientChannel
import org.dweb_browser.core.http.PureFrame
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureServerChannel
import org.dweb_browser.core.http.PureStream
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.IpcRequestInit
import org.dweb_browser.core.ipc.debugIpc
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.eprintln
import org.dweb_browser.helper.falseAlso
import kotlin.coroutines.coroutineContext


const val PURE_CHANNEL_EVENT_PREFIX = "λ"
const val X_IPC_UPGRADE_KEY = "X-Dweb-Ipc-Upgrade-Key"

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

    /**
     * 一个将 pureChannel 与 ipc 进行关联转换的函数
     *
     * TODO 这里应该使用 fork():Ipc 来承载 pureChannel
     * 目前这里使用一个 ipcEvent 来承载 pureChannel，行为上比较奇怪，性能上也不是最佳
     */
    private suspend fun pureChannelToIpcEvent(
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
      ipc.onEvent { (ipcEvent) ->
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
            offListener()
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
    }


    private val duplex by lazy { SafeInt(0) }

    suspend fun fromPure(
      req_id: Int,
      ipc: Ipc,
      pureRequest: PureRequest,
      isIpcSender: Boolean,
    ): IpcRequest {
      if (pureRequest.hasChannel) {
        val eventNameBase = "$PURE_CHANNEL_EVENT_PREFIX-${ipc.uid}/${req_id}/${duplex.inc().value}"

        debugIpc("fromPure/hasChannel") { "create ipcEventBaseName:$eventNameBase => request:$pureRequest" }
        CoroutineScope(coroutineContext).launch {
          val pureChannel = pureRequest.getChannel()
          debugIpc("fromPure/channelToIpc") { "channelId:$eventNameBase => pureChannel:$pureChannel start!!" }

          /// 不论是请求者还是响应者
          /// 那么意味着数据需要通过ipc来进行发送。所以我需要将 pureChannel 中要发送的数据读取出来进行发送
          /// 反之，ipc收到的数据也要作为 pureChannel 的
          val channelContext = pureChannel.start()
          pureChannelToIpcEvent(
            eventNameBase,
            ipc,
            pureChannel,
            channelByIpcEmit = channelContext.outgoing,
            channelForIpcPost = channelContext.income,
            _debugTag = "fromPure/channelToIpc"
          ) { }
        }

        val ipcRequest = fromRequest(
          req_id, ipc, pureRequest.href,
          IpcRequestInit(
            pureRequest.method,
            IPureBody.Empty,
            pureRequest.headers.copy().apply {
              init(X_IPC_UPGRADE_KEY, eventNameBase).falseAlso {
                eprintln("fromPure WARNING: SHOULD NOT HAPPENED, PURE_REQUEST CONTAINS 'X_IPC_UPGRADE_KEY' IN HEADERS")
              }
            })
        )

        when {
          isIpcSender -> ipcRequest._pureClient
          else -> ipcRequest._pureServer
        }.update { pureRequest }
        return ipcRequest
      }
      return fromRequest(
        req_id, ipc, pureRequest.href,
        IpcRequestInit(pureRequest.method, pureRequest.body, pureRequest.headers)
      )
    }
  }

  /**
   * 判断是否是双工协议
   *
   * 注意，这里WebSocket，但是可以由 WebSocket 来提供支持
   * WebSocket 是在头部中有 Upgrade ，我们这里是 X_IPC_PURE_CHANNEL_ID
   */
  val hasDuplex get() = duplexEventBaseName != null
  private val duplexEventBaseName by lazy {
    var eventNameBase: String? = null
    headers.get(X_IPC_UPGRADE_KEY)?.also {
      if (it.startsWith(PURE_CHANNEL_EVENT_PREFIX)) {
        eventNameBase = it
      }
    }
    eventNameBase
  }


  private val _pureClient by lazy { atomic<PureRequest?>(null) }
  private val _pureServer by lazy { atomic<PureRequest?>(null) }

  /**
   * 如果 isIpcSender = true，那么处理 ipcForChannelHandler 的就是 ipcRequest 自身的 ipc 对象。
   * 否则，ipcForChannelHandler 应该由外部传入，传入 onMessage/onRequest 的处理者
   */
  suspend fun toPure(isIpcSender: Boolean, ipcForChannelHandler: Ipc = ipc) = when {
    // 如果作为ipcRequest的请求发起者
    isIpcSender -> _pureClient
    // 如果作为ipcRequest的请求处理者
    else -> _pureServer
  }.updateAndGet {
    if (!isIpcSender) {
      require(
        ipcForChannelHandler != ipc
      ) { "ipcForChannelHandler 应该由外部传入，传入 onMessage/onRequest 的处理者" }
    }
    it ?: buildRequestX(url, method, headers, body.raw).let { pureRequest ->
      if (hasDuplex) {
        debugIpc(
          "toPure/ipcToChannel",
          "isIpcSender:$isIpcSender channelId:$duplexEventBaseName => request:$this start!!"
        )

        val income = Channel<PureFrame>()
        val outgoing = Channel<PureFrame>()
        val pureChannel = when {
          isIpcSender -> PureClientChannel(income, outgoing, pureRequest, from = this)
          else -> PureServerChannel(income, outgoing, pureRequest, from = this)
        }
        CoroutineScope(coroutineContext).launch {
          pureChannelToIpcEvent(
            duplexEventBaseName!!,
            ipcForChannelHandler,
            pureChannel,
            channelByIpcEmit = income,
            channelForIpcPost = outgoing,
            _debugTag = "toPure/ipcToChannel",
          ) { pureChannel.afterStart(); }
        }

        pureRequest.copy(
          channel = CompletableDeferred(),
          headers = pureRequest.headers.copy().apply { delete(X_IPC_UPGRADE_KEY) })
          .apply { initChannel(pureChannel) }
      } else pureRequest
    }
  }!!

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
