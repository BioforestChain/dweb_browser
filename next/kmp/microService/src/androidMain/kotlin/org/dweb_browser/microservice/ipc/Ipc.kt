package org.dweb_browser.microservice.ipc

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.defaultAsyncExceptionHandler
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.help.types.IMicroModuleManifest
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.dweb_browser.microservice.ipc.helper.IpcEventMessageArgs
import org.dweb_browser.microservice.ipc.helper.IpcMessage
import org.dweb_browser.microservice.ipc.helper.IpcMessageArgs
import org.dweb_browser.microservice.ipc.helper.IpcRequest
import org.dweb_browser.microservice.ipc.helper.IpcRequestMessageArgs
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.dweb_browser.microservice.ipc.helper.IpcResponseMessageArgs
import org.dweb_browser.microservice.ipc.helper.IpcStream
import org.dweb_browser.microservice.ipc.helper.IpcStreamMessageArgs
import org.dweb_browser.microservice.ipc.helper.OnIpcEventMessage
import org.dweb_browser.microservice.ipc.helper.OnIpcMessage
import org.dweb_browser.microservice.ipc.helper.OnIpcRequestMessage
import org.dweb_browser.microservice.ipc.helper.OnIpcResponseMessage
import org.dweb_browser.microservice.ipc.helper.OnIpcStreamMessage
import org.http4k.core.Body
import org.http4k.core.Headers
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import java.util.concurrent.atomic.AtomicInteger

abstract class Ipc {
  companion object {
    private var uid_acc = AtomicInteger(1)
    private var req_id_acc = AtomicInteger(0);
    private val ipcMessageCoroutineScope =
      CoroutineScope(CoroutineName("ipc-message") + ioAsyncExceptionHandler)
  }

  val uid = uid_acc.getAndAdd(1)

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

  fun asRemoteInstance() = if (remote is MicroModule) remote as MicroModule else null


  abstract val role: String

  override fun toString() = "#i$uid"

  suspend fun postMessage(message: IpcMessage) {
    if (this._closed) {
      return;
    }
    this._doPostMessage(message);
  }


  suspend fun postResponse(req_id: Int, response: Response) {
    postMessage(
      IpcResponse.fromResponse(
        req_id,
        response,
        this
      )
    )
  }

  protected val _messageSignal = Signal<IpcMessageArgs>();
  fun onMessage(cb: OnIpcMessage) = _messageSignal.listen(cb)

  /**
   * 强制触发消息传入，而不是依赖远端的 postMessage
   */
  suspend fun emitMessage(args: IpcMessageArgs) = _messageSignal.emit(args)

  abstract suspend fun _doPostMessage(data: IpcMessage): Unit;

  private fun <T : Any> _createSignal(): Signal<T> {
    val signal = Signal<T>()
    this.onClose {
      signal.clear()
    }
    return signal;
  }

  private val _requestSignal by lazy {
    _createSignal<IpcRequestMessageArgs>().also { signal ->
      _messageSignal.listen { args ->
        if (args.message is IpcRequest) {
          ipcMessageCoroutineScope.launch {
            signal.emit(
              IpcRequestMessageArgs(
                args.message,
                args.ipc
              )
            );
          }
        }
      }
    }
  }

  fun onRequest(cb: OnIpcRequestMessage) = _requestSignal.listen(cb)

  private val _responseSignal by lazy {
    _createSignal<IpcResponseMessageArgs>().also { signal ->
      _messageSignal.listen { args ->
        if (args.message is IpcResponse) {
          ipcMessageCoroutineScope.launch {
            signal.emit(
              IpcResponseMessageArgs(
                args.message,
                args.ipc
              )
            );
          }
        }
      }
    }
  }

  fun onResponse(cb: OnIpcResponseMessage) = _responseSignal.listen(cb)

  private val _streamSignal by lazy {
    val signal = _createSignal<IpcStreamMessageArgs>()
    /// 这里建立起一个独立的顺序队列，目的是避免处理阻塞
    /// TODO 这里不应该使用 UNLIMITED，而是压力到一定程度方向发送限流的指令
    val streamChannel = Channel<IpcStreamMessageArgs>(capacity = Channel.UNLIMITED)
    ipcMessageCoroutineScope.launch {
      for (message in streamChannel) {
        signal.emit(message);
      }
    }
    _messageSignal.listen { args ->
      if (args.message is IpcStream) {
        streamChannel.trySend(
          IpcStreamMessageArgs(
            args.message,
            args.ipc
          )
        )
      }
    }
    onClose {
      streamChannel.close();
    }
    signal
  }

  fun onStream(cb: OnIpcStreamMessage) = _streamSignal.listen(cb)

  private val _eventSignal by lazy {
    _createSignal<IpcEventMessageArgs>().also { signal ->
      _messageSignal.listen { args ->
        if (args.message is IpcEvent) {
          ipcMessageCoroutineScope.launch {
            signal.emit(
              IpcEventMessageArgs(
                args.message,
                args.ipc
              )
            );
          }
        }
      }
    }
  }

  fun onEvent(cb: OnIpcEventMessage) = _eventSignal.listen(cb)


  abstract suspend fun _doClose(): Unit;

  private var _closed = false
  suspend fun close() {
    if (this._closed) {
      return;
    }
    this._closed = true
    this._doClose()
    this.closeSignal.emitAndClear()

    /// 关闭的时候会自动触发销毁
    this.destroy(false)
  }

  val isClosed get() = _closed

  val closeSignal = SimpleSignal();
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
  suspend fun request(url: String) =
    request(Request(Method.GET, url))

  suspend fun request(url: Uri) =
    request(Request(Method.GET, url))

  private val _reqResMap by lazy {
    mutableMapOf<Int, PromiseOut<IpcResponse>>().also { reqResMap ->
      onResponse { (response) ->
        val result = reqResMap.remove(response.req_id)
          ?: throw Exception("no found response by req_id: ${response.req_id}")
        result.resolve(response)
      }
    }
  }

  suspend fun request(ipcRequest: IpcRequest): IpcResponse {
    val result = PromiseOut<IpcResponse>();
    _reqResMap[ipcRequest.req_id] = result;
    this.postMessage(ipcRequest)
    return result.waitPromise()
  }

  fun _buildIpcRequest(url: String, init: IpcRequestInit): IpcRequest {
    val req_id = this.allocReqId();
    val ipcRequest = IpcRequest.fromRequest(req_id, this, url, init);
    return ipcRequest;
  }

  suspend fun request(request: Request) =
    this.request(
      IpcRequest.fromRequest(
        allocReqId(),
        this,
        request.uri.toString(),
        IpcRequestInit(request.method, request.body, request.headers)
      )
    ).toResponse()


  suspend fun request(url: String, init: IpcRequestInit): IpcResponse {
    val ipcRequest = this._buildIpcRequest(url, init)
    val result = this.registerReqId(ipcRequest.req_id);
    this.postMessage(ipcRequest);
    return result.waitPromise();
  }

  fun allocReqId() = req_id_acc.getAndAdd(1)

  /** 自定义注册 请求与响应 的id */
  fun registerReqId(req_id: Int = this.allocReqId()): PromiseOut<IpcResponse> {
    return _reqResMap.getOrPut(req_id) {
      return PromiseOut()
    }
  };

  /// 应用级别的 Ready协议，使用ping-pong方式来等待对方准备完毕，这不是必要的，确保双方都准寻这个协议才有必要去使用
  /// 目前使用这个协议的主要是Web端（它同时还使用了 Activity协议）
  private val readyListener by lazy {
    val ready = PromiseOut<IpcEvent>()
    this.onEvent { (event, ipc) ->
      if (event.name == "ping") {
        ipc.postMessage(IpcEvent("pong", event.data, event.encoding))
        ready.resolve(event)
      } else if (event.name == "pong") {
        ready.resolve(event)
      }
    }
    CoroutineScope(defaultAsyncExceptionHandler).launch {
      val ipc = this@Ipc
      while (!ready.isResolved && !ipc.isClosed) {
        ipc.postMessage(IpcEvent.fromUtf8("ping", ""))
        delay(1000)
      }
    }
    ready
  }

  suspend fun ready(self: MicroModule) {
    this.readyListener.waitPromise();// get once
  }
}

data class IpcRequestInit(
  var method: Method = Method.GET,
  var body: Body = Body.EMPTY,
  var headers: Headers = listOf()
)
