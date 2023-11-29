package org.dweb_browser.core.ipc

import io.ktor.http.Url
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.http.IPureBody
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcEventMessageArgs
import org.dweb_browser.core.ipc.helper.IpcHeaders
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.core.ipc.helper.IpcMessageArgs
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.ipc.helper.IpcRequest
import org.dweb_browser.core.ipc.helper.IpcRequestMessageArgs
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.ipc.helper.IpcResponseMessageArgs
import org.dweb_browser.core.ipc.helper.IpcStream
import org.dweb_browser.core.ipc.helper.IpcStreamMessageArgs
import org.dweb_browser.core.ipc.helper.OnIpcEventMessage
import org.dweb_browser.core.ipc.helper.OnIpcMessage
import org.dweb_browser.core.ipc.helper.OnIpcRequestMessage
import org.dweb_browser.core.ipc.helper.OnIpcResponseMessage
import org.dweb_browser.core.ipc.helper.OnIpcStreamMessage
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.defaultAsyncExceptionHandler
import org.dweb_browser.helper.ioAsyncExceptionHandler

val debugIpc = Debugger("ipc")

abstract class Ipc {
  companion object {
    private var uid_acc by SafeInt(1)
    private var req_id_acc by SafeInt(0)
    private val ipcMessageCoroutineScope =
      CoroutineScope(CoroutineName("ipc-message") + ioAsyncExceptionHandler)
  }

  val uid = uid_acc++

  /**
   * 是否支持 messagePack 协议传输：
   * 需要同时满足两个条件：通道支持直接传输二进制；通达支持 MessagePack 的编解码
   */
  open val supportMessagePack: Boolean = false

  /**
   * 是否支持 Protobuf 协议传输：
   * 需要同时满足两个条件：通道支持直接传输二进制；通达支持 Protobuf 的编解码
   */
  open val supportProtobuf: Boolean = false

  /**
   * 是否支持结构化内存协议传输：
   * 就是说不需要对数据手动序列化反序列化，可以直接传输内存对象
   */
  open val supportRaw: Boolean = false

  /** 是否支持 二进制 传输 */
  open val supportBinary: Boolean = false // get() = supportMessagePack || supportProtobuf

  abstract val remote: IMicroModuleManifest

  fun remoteAsInstance() = if (remote is MicroModule) remote as MicroModule else null


  abstract val role: String

  override fun toString() = "#i$uid"

  suspend fun postMessage(message: IpcMessage) {
    if (this._closed) {
      debugIpc("fail to post message, already closed")
      return
    }
    this._doPostMessage(message)
  }


  suspend fun postResponse(req_id: Int, response: PureResponse) {
    postMessage(
      IpcResponse.fromResponse(
        req_id, response, this
      )
    )
  }

  protected val _messageSignal = Signal<IpcMessageArgs>()
  fun onMessage(cb: OnIpcMessage) = _messageSignal.listen(cb)

  /**
   * 强制触发消息传入，而不是依赖远端的 postMessage
   */
  suspend fun emitMessage(args: IpcMessageArgs) = _messageSignal.emit(args)

  abstract suspend fun _doPostMessage(data: IpcMessage)

  private fun <T : Any> _createSignal(): Signal<T> {
    val signal = Signal<T>()
    this.onClose {
      signal.clear()
    }
    return signal
  }

  private val _requestSignal by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    _createSignal<IpcRequestMessageArgs>().also { signal ->
      _messageSignal.listen { args ->
        if (args.message is IpcRequest) {
          ipcMessageCoroutineScope.launch {
            signal.emit(
              IpcRequestMessageArgs(args.message, args.ipc)
            )
          }
        }
      }
    }
  }

  fun onRequest(cb: OnIpcRequestMessage) = _requestSignal.listen(cb)

  private val _responseSignal by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    _createSignal<IpcResponseMessageArgs>().also { signal ->
      _messageSignal.listen { args ->
        if (args.message is IpcResponse) {
          ipcMessageCoroutineScope.launch {
            signal.emit(
              IpcResponseMessageArgs(
                args.message, args.ipc
              )
            )
          }
        }
      }
    }
  }

  private fun onResponse(cb: OnIpcResponseMessage) = _responseSignal.listen(cb)

  private val _streamSignal by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    val signal = _createSignal<IpcStreamMessageArgs>()
    /// 这里建立起一个独立的顺序队列，目的是避免处理阻塞
    /// TODO 这里不应该使用 UNLIMITED，而是压力到一定程度方向发送限流的指令
    val streamChannel = Channel<IpcStreamMessageArgs>(capacity = Channel.UNLIMITED)
    ipcMessageCoroutineScope.launch {
      for (message in streamChannel) {
        signal.emit(message)
      }
    }
    _messageSignal.listen { args ->
      if (args.message is IpcStream) {
        streamChannel.trySend(
          IpcStreamMessageArgs(
            args.message, args.ipc
          )
        )
      }
    }
    onClose {
      streamChannel.close()
    }
    signal
  }

  fun onStream(cb: OnIpcStreamMessage) = _streamSignal.listen(cb)

  private val _eventSignal by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    _createSignal<IpcEventMessageArgs>().also { signal ->
      _messageSignal.listen { args ->
        if (args.message is IpcEvent) {
          ipcMessageCoroutineScope.launch {
            signal.emit(
              IpcEventMessageArgs(
                args.message, args.ipc
              )
            )
          }
        }
      }
    }
  }

  fun onEvent(cb: OnIpcEventMessage) = _eventSignal.listen(cb)


  abstract suspend fun _doClose(): Unit

  private var _closed = false
  suspend fun close() {
    if (this._closed) {
      return
    }
    this._closed = true
    this._doClose()
    this.closeSignal.emitAndClear()

    /// 关闭的时候会自动触发销毁
    this.destroy(false)
  }

  val isClosed get() = _closed

  val closeSignal = SimpleSignal()
  val onClose = this.closeSignal.toListener()


  private val _destroySignal = SimpleSignal()
  val onDestroy = this._destroySignal.toListener()

  private var _destroyed = false
  val isDestroy get() = _destroyed

  /**
   * 销毁实例
   */
  suspend fun destroy(close: Boolean = true) {
    if (_destroyed) {
      return
    }
    _destroyed = true
    if (close) {
      this.close()
    }
    this._destroySignal.emitAndClear()
  }

  /**
   * 发送请求
   */
  suspend fun request(url: String) = request(PureRequest(method = IpcMethod.GET, href = url))

  suspend fun request(url: Url) =
    request(PureRequest(method = IpcMethod.GET, href = url.toString()))

  private val _reqResMap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    SafeHashMap<Int, CompletableDeferred<IpcResponse>>().also { reqResMap ->
      onResponse { (response) ->
        val result = reqResMap.remove(response.req_id)
          ?: throw Exception("no found response by req_id: ${response.req_id}")
        result.complete(response)
      }
    }
  }

  suspend fun request(ipcRequest: IpcRequest): IpcResponse {
    val result = CompletableDeferred<IpcResponse>()
    _reqResMap[ipcRequest.req_id] = result
    this.postMessage(ipcRequest)
    return result.await()
  }

  private fun _buildIpcRequest(url: String, init: IpcRequestInit): IpcRequest {
    val reqId = this.allocReqId()
    return IpcRequest.fromRequest(reqId, this, url, init)
  }

  suspend fun request(request: PureRequest) = this.request(
    IpcRequest.fromRequest(
      allocReqId(),
      this,
      request.href,
      IpcRequestInit(request.method, request.body, request.headers)
    )
  ).toResponse()


  suspend fun request(url: String, init: IpcRequestInit): IpcResponse {
    val ipcRequest = this._buildIpcRequest(url, init)
    return request(ipcRequest)
  }

  private val reqIdSyncObj = SynchronizedObject()
  private fun allocReqId() = synchronized(reqIdSyncObj) { ++req_id_acc }

  /** 自定义注册 请求与响应 的id */
  private fun registerReqId(req_id: Int = this.allocReqId()): CompletableDeferred<IpcResponse> {
    return _reqResMap.getOrPut(req_id) {
      return CompletableDeferred()
    }
  }

  /// 应用级别的 Ready协议，使用ping-pong方式来等待对方准备完毕，这不是必要的，确保双方都准寻这个协议才有必要去使用
  /// 目前使用这个协议的主要是Web端（它同时还使用了 Activity协议）
  private val readyListener by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    val ready = CompletableDeferred<IpcEvent>()
    this.onEvent { (event, ipc) ->

      if (event.name == "ping") {
        ipc.postMessage(IpcEvent("pong", event.data, event.encoding))
      } else if (event.name == "pong") {
        ready.complete(event)
      }
    }
    CoroutineScope(defaultAsyncExceptionHandler).launch {
      val ipc = this@Ipc
      val pingDelay = 200L
      var timeout = 30000L
      while (!ready.isCompleted && !ipc.isClosed && timeout > 0L) {
        ipc.postMessage(IpcEvent.fromUtf8("ping", ""))
        delay(pingDelay)
        timeout -= pingDelay
      }
    }
    ready
  }

  suspend fun ready(self: MicroModule) {
    this.readyListener.await()// get once
  }
}

data class IpcRequestInit(
  var method: IpcMethod = IpcMethod.GET,
  var body: IPureBody = IPureBody.Empty,
  var headers: IpcHeaders = IpcHeaders()
)
