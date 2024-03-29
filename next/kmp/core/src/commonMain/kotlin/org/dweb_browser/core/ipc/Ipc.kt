package org.dweb_browser.core.ipc

import io.ktor.http.Url
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.helper.IPC_STATE
import org.dweb_browser.core.ipc.helper.IpcClientRequest
import org.dweb_browser.core.ipc.helper.IpcClientRequest.Companion.toIpc
import org.dweb_browser.core.ipc.helper.IpcError
import org.dweb_browser.core.ipc.helper.IpcErrorMessageArgs
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcEventMessageArgs
import org.dweb_browser.core.ipc.helper.IpcLifeCycle
import org.dweb_browser.core.ipc.helper.IpcLifeCycleMessageArgs
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.core.ipc.helper.IpcMessageArgs
import org.dweb_browser.core.ipc.helper.IpcRequest
import org.dweb_browser.core.ipc.helper.IpcRequestMessageArgs
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.ipc.helper.IpcResponseMessageArgs
import org.dweb_browser.core.ipc.helper.IpcServerRequest
import org.dweb_browser.core.ipc.helper.IpcStream
import org.dweb_browser.core.ipc.helper.IpcStreamMessageArgs
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.withScope
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse

val debugIpc = Debugger("ipc")

//fun <T> Flow<T>.toListener(launchInScope: CoroutineScope) = FlowListener(this, launchInScope)
//class FlowListener<T>(private val flow: Flow<T>, private val launchInScope: CoroutineScope) {
//  operator fun invoke(cb: suspend (T) -> Unit) {
//    flow.onEach(cb).launchIn(launchInScope)
//  }
//}

/**
 * 抽象工厂模式
 */
abstract class Ipc(val channelId: String, val endpoint: IpcPool) {
  companion object {
    private var uid_acc by SafeInt(1)
    private var reqId_acc by SafeInt(0)
    var order_by_acc by SafeInt(0)
  }

  val ipcScope = CoroutineScope(CoroutineName("ipc-$channelId") + ioAsyncExceptionHandler)

  abstract val remote: IMicroModuleManifest
  fun remoteAsInstance() = if (remote is MicroModule) remote as MicroModule else null


  val uid = uid_acc++
  private val pid = endpoint.generatePid(channelId)

  private var ipcLifeCycleState: IPC_STATE = IPC_STATE.OPENING

  /**-----protocol support start*/
  /**
   * 是否支持 cbor 协议传输：
   * 需要同时满足两个条件：通道支持直接传输二进制；通达支持 cbor 的编解码
   */
  open val supportCbor: Boolean = false

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
  open val supportBinary: Boolean = false // get() = supportCbor || supportProtobuf

  /**-----protocol support end*/
  override fun toString() = "Ipc#state=$ipcLifeCycleState,channelId=$channelId"


  /**-----onMessage start*/

  private fun <T : Any> messagePipeMap(transform: suspend (value: IpcMessageArgs) -> T?) =
    messageFlow.mapNotNull(transform).shareIn(ipcScope, SharingStarted.Lazily)

  val requestFlow by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap { args ->
      when (val ipcReq = args.message) {
        is IpcRequest -> {
          val ipcServerRequest = when (ipcReq) {
            is IpcClientRequest -> ipcReq.toServer(args.ipc)
            is IpcServerRequest -> ipcReq
          }
          IpcRequestMessageArgs(ipcServerRequest, args.ipc)
        }

        else -> null
      }
    }
  }

  private val responseFlow by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap {
      if (it.message is IpcResponse) {
        IpcResponseMessageArgs(it.message, it.ipc)
      } else null
    }
  }

  val streamFlow by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap {
      if (it.message is IpcStream) {
        IpcStreamMessageArgs(it.message, it.ipc)
      } else null
    }
  }

  val eventFlow by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap { args ->
      if (args.message is IpcEvent) {
        IpcEventMessageArgs(
          args.message, args.ipc
        )
      } else null
    }
  }

  val lifeCyCleFlow by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap { args ->
      if (args.message is IpcLifeCycle) {
        IpcLifeCycleMessageArgs(
          args.message, args.ipc
        )
      } else null
    }
  }

  val errorFlow by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap { args ->
      if (args.message is IpcError) {
        IpcErrorMessageArgs(
          args.message, args.ipc
        )
      } else null
    }
  }

  /**-----onMessage end*/


  /**----- 发送请求 start */
  suspend fun request(url: String) = request(PureClientRequest(method = PureMethod.GET, href = url))

  suspend fun request(url: Url) =
    request(PureClientRequest(method = PureMethod.GET, href = url.toString()))

  suspend fun postResponse(reqId: Int, response: PureResponse) {
    postMessage(
      IpcResponse.fromResponse(
        reqId, response, this
      )
    )
  }

  private val _reqResMap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    SafeHashMap<Int, CompletableDeferred<IpcResponse>>().also { reqResMap ->
      responseFlow.onEach { (response) ->
        val result = reqResMap.remove(response.reqId)
          ?: throw Exception("no found response by reqId: ${response.reqId}")
        result.complete(response)
      }.launchIn(ipcScope)
    }
  }

  suspend fun request(ipcRequest: IpcRequest): IpcResponse {
    val result = CompletableDeferred<IpcResponse>()
    _reqResMap[ipcRequest.reqId] = result
    this.postMessage(ipcRequest)
    return result.await()
  }

  private suspend fun _buildIpcRequest(url: String, init: IpcRequestInit): IpcRequest {
    val reqId = this.allocReqId()
    return IpcClientRequest.fromRequest(reqId, this, url, init)
  }

  // PureClientRequest -> ipcRequest -> IpcResponse -> PureResponse
  suspend fun request(pureRequest: PureClientRequest): PureResponse {
    return this.request(
      pureRequest.toIpc(allocReqId(), this)
    ).toPure()
  }

  suspend fun request(url: String, init: IpcRequestInit): IpcResponse {
    val ipcRequest = this._buildIpcRequest(url, init)
    return request(ipcRequest)
  }

  private val reqIdSyncObj = SynchronizedObject()
  private fun allocReqId() = synchronized(reqIdSyncObj) { ++reqId_acc }

  /**----- 发送请求 end */

  // 发消息
  protected abstract suspend fun doPostMessage(pid: Int, data: IpcMessage)

  /**发送各类消息到remote*/
  suspend fun postMessage(data: IpcMessage) {
    if (isClosed) {
      debugIpcPool("ipc postMessage", "[$channelId] already closed:discard $data")
      return
    }
    // 等待通信建立完成（如果通道没有建立完成，并且不是生命周期消息）
    if (!isActivity && data !is IpcLifeCycle) {
      awaitStart()
    }
//    println("分发消息=> $data")
    withScope(ipcScope) {
      // 分发消息
      doPostMessage(pid, data)
    }
  }

  // Flow 对象本身并不持有任何状态，它只是一个冷数据流。真正持有状态的是 collect 的协程。所以，理论上来说，不需要特地去清空或注销 Flow。
  // 如果你想停止数据流，你可以考虑取消消费这个 Flow 的协程。在你的协程被取消后，Flow 自然就停止了
  val messageFlow = MutableSharedFlow<IpcMessageArgs>(
    replay = 10,//相当于粘性数据
    extraBufferCapacity = 10,//接受的慢时候，发送的入栈 防止有一个请求挂起的时候 app其他请求无法进行
    onBufferOverflow = BufferOverflow.SUSPEND // 缓冲区溢出的时候挂起 背压
  )

  /**分发各类消息到本地*/
  suspend fun dispatchMessage(args: IpcMessageArgs) = messageFlow.emit(args)
  internal suspend fun dispatchMessage(ipcMessage: IpcMessage) =
    messageFlow.emit(IpcMessageArgs(ipcMessage, this))

  // 标记是否启动完成
  val startDeferred = CompletableDeferred<IpcLifeCycle>()

  // 标记ipc通道是否激活
  val isActivity get() = startDeferred.isCompleted

  suspend fun awaitStart() = startDeferred.await()

  // 告知对方我启动了
  suspend fun start() {
    ipcLifeCycleState = IPC_STATE.OPEN
    // 连接成功不管先后发送请求
    this.postMessage(IpcLifeCycle.opening())
  }

  /**生命周期初始化，协商数据格式*/
  fun initLifeCycleHook() {
    // TODO 跟对方通信 协商数据格式
    println("xxlife onLifeCycle=>🍃  $channelId ${this.remote.mmid}")
    lifeCyCleFlow.onEach { (lifeCycle, ipc) ->
      when (lifeCycle.state) {
        // 收到对方完成开始建立连接
        IPC_STATE.OPENING -> {
          println("xxlife onLifeCycle OPENING=>🍟  ${ipc.channelId} ${lifeCycle.state}")
          ipc.postMessage(IpcLifeCycle.open()) // 解锁对方的
          ipc.startDeferred.complete(lifeCycle) // 解锁自己的
        }

        IPC_STATE.OPEN -> {
          println("xxlife onLifeCycle OPEN=>🍟  ${ipc.channelId} ${lifeCycle.state}")
          if (!ipc.startDeferred.isCompleted) {
            ipc.startDeferred.complete(lifeCycle)
          }
        }
        // 消息通道开始关闭
        IPC_STATE.CLOSING -> {
          debugIpc("🌼IPC close", "$channelId ${ipc.remote.mmid}")
          // 接收方接收到对方请求关闭了
          ipcLifeCycleState = IPC_STATE.CLOSING
          ipc.postMessage(IpcLifeCycle.close())
          ipc.close()
        }
        // 对方关了，代表没有消息发过来了，我也关闭
        IPC_STATE.CLOSED -> {
          debugIpc("🌼IPC destroy", "$channelId ${ipc.remote.mmid} $isClosed")
          ipc.doClose()
        }
      }
    }.launchIn(ipcScope)
  }

  /**----- close start*/

  val isClosed get() = ipcLifeCycleState == IPC_STATE.CLOSED

  abstract suspend fun _doClose()

  // 告知对方，我这条业务线已经准备关闭了
  private suspend fun tryClose() {
    if (ipcLifeCycleState < IPC_STATE.CLOSING) {
      ipcLifeCycleState = IPC_STATE.CLOSING
      this.postMessage(IpcLifeCycle(IPC_STATE.CLOSING))
    }
  }

  // 开始触发关闭事件
  fun close() = SuspendOnce {
    this.tryClose()
    if (!isClosed) {
      this.doClose()
    }
  }

  private val closeSignal = CompletableDeferred<CancellationException?>()

  val closeDeferred = closeSignal as Deferred<CancellationException?>
//  suspend fun onClose(cb: () -> Unit) {
//    closeDeferred.await()
//    cb()
//  }


  //彻底销毁
  private val doClose = SuspendOnce {
    // 做完全部工作了，关闭
    ipcLifeCycleState = IPC_STATE.CLOSING
    // 我彻底关闭了
    this.postMessage(IpcLifeCycle.close())
    // 开始触发各类跟ipc绑定的关闭事件
    this.closeSignal.complete(null)
    debugIpc("ipcDestroy=>", " $channelId 触发完成")
    // 做完全部工作了，关闭
    ipcLifeCycleState = IPC_STATE.CLOSED
    // 关闭通信信道
    this._doClose()
    ipcScope.cancel()
  }
  /**----- close end*/
}

data class IpcRequestInit(
  var method: PureMethod = PureMethod.GET,
  var body: IPureBody = IPureBody.Empty,
  var headers: PureHeaders = PureHeaders()
)
