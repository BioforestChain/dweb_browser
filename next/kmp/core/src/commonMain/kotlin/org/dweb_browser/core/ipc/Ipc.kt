package org.dweb_browser.core.ipc

import io.ktor.http.Url
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.plus
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.helper.ENDPOINT_STATE
import org.dweb_browser.core.ipc.helper.EndpointLifecycle
import org.dweb_browser.core.ipc.helper.EndpointIpcMessage
import org.dweb_browser.core.ipc.helper.IpcClientRequest
import org.dweb_browser.core.ipc.helper.IpcClientRequest.Companion.toIpc
import org.dweb_browser.core.ipc.helper.IpcError
import org.dweb_browser.core.ipc.helper.IpcErrorMessageArgs
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcEventMessageArgs
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
class Ipc(val remote: IMicroModuleManifest, val endpoint: IpcEndpoint, val pool: IpcPool) {
  companion object {
    private var uid_acc by SafeInt(1)
    private var reqId_acc by SafeInt(0)
    var order_by_acc by SafeInt(0)
  }

  val uid = uid_acc++
  private val pid = pool.generatePid()

  val scope = endpoint.scope + Job()

  val ipcDebugId: String = "$pid/${endpoint.endpointDebugId}"

  override fun toString() = "Ipc#$ipcDebugId"

  fun remoteAsInstance() = if (remote is MicroModule) remote else null

  // FIXME 这里两个10应该移除
  val messageFlow = MutableSharedFlow<IpcMessageArgs>(
    replay = 10,//相当于粘性数据
    extraBufferCapacity = 10,//接受的慢时候，发送的入栈 防止有一个请求挂起的时候 app其他请求无法进行
    onBufferOverflow = BufferOverflow.SUSPEND // 缓冲区溢出的时候挂起 背压
  )

  /**-----onMessage start*/

  private inline fun <T : Any> messagePipeMap(crossinline transform: suspend (value: IpcMessageArgs) -> T?) =
    messageFlow.mapNotNull(transform).shareIn(scope, SharingStarted.Eagerly)

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

  val onResponse by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap {
      if (it.message is IpcResponse) {
        IpcResponseMessageArgs(it.message, it.ipc)
      } else null
    }
  }

  val onStream by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap {
      if (it.message is IpcStream) {
        IpcStreamMessageArgs(it.message, it.ipc)
      } else null
    }
  }

  val onEvent by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap { args ->
      if (args.message is IpcEvent) {
        IpcEventMessageArgs(
          args.message, args.ipc
        )
      } else null
    }
  }


  val onError by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
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
  suspend inline fun request(url: String) = request(
    PureClientRequest(
      method = PureMethod.GET,
      href = url,
    )
  )
  suspend inline fun request(url: Url) = request(url.toString())
  suspend fun request(ipcRequest: IpcRequest): IpcResponse {
    val result = CompletableDeferred<IpcResponse>()
    _reqResMap[ipcRequest.reqId] = result
    this.postMessage(ipcRequest)
    return result.await()
  }


  suspend fun postResponse(reqId: Int, response: PureResponse) {
    postMessage(
      IpcResponse.fromResponse(
        reqId, response, this
      )
    )
  }

  private val _reqResMap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    SafeHashMap<Int, CompletableDeferred<IpcResponse>>().also { reqResMap ->
      onResponse.onEach { (response) ->
        val result = reqResMap.remove(response.reqId)
          ?: throw Exception("no found response by reqId: ${response.reqId}")
        result.complete(response)
      }.launchIn(scope)
    }
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
  suspend fun doPostMessage(data: IpcMessage) = withScope(scope) {
    endpoint.postMessage(EndpointIpcMessage(pid, data))
  }

  /**发送各类消息到remote*/
  suspend fun postMessage(data: IpcMessage) {
    withScope(scope) {
      endpoint.postMessage(EndpointIpcMessage(pid, data))
    }
  }


  /**分发各类消息到本地*/
  suspend fun dispatchMessage(args: IpcMessageArgs) = messageFlow.emit(args)
  internal suspend fun dispatchMessage(ipcMessage: IpcMessage) =
    messageFlow.emit(IpcMessageArgs(ipcMessage, this))

  // 标记ipc通道是否激活
  val isActivity get() = endpoint.isActivity

  suspend fun awaitOpen() = endpoint.awaitOpen()

  // 告知对方我启动了
  suspend fun start() {
    withScope(scope) {
      endpoint.launchSyncLifecycle()
    }
  }

  /**生命周期初始化，协商数据格式*/
  fun initLifeCycleHook() {
    // TODO 跟对方通信 协商数据格式
    println("xxlife onLifeCycle=>🍃  $ipcDebugId ${this.remote.mmid}")
    lifeCyCleFlow.onEach { (lifeCycle, ipc) ->
      when (lifeCycle.state) {
        // 收到对方完成开始建立连接
        ENDPOINT_STATE.OPENING -> {
          println("xxlife onLifeCycle OPENING=>🍟  ${ipc.ipcDebugId} ${lifeCycle.state}")
          ipc.postMessage(EndpointLifecycle.open()) // 解锁对方的
          ipc.startDeferred.complete(lifeCycle) // 解锁自己的
        }

        ENDPOINT_STATE.OPENED -> {
          println("xxlife onLifeCycle OPEN=>🍟  ${ipc.ipcDebugId} ${lifeCycle.state}")
          if (!ipc.startDeferred.isCompleted) {
            ipc.startDeferred.complete(lifeCycle)
          }
        }
        // 消息通道开始关闭
        ENDPOINT_STATE.CLOSING -> {
          debugIpc("🌼IPC close", "$ipcDebugId ${ipc.remote.mmid}")
          // 接收方接收到对方请求关闭了
          ipcLifeCycleState = ENDPOINT_STATE.CLOSING
          ipc.postMessage(EndpointLifecycle.Closed())
          ipc.close()
        }
        // 对方关了，代表没有消息发过来了，我也关闭
        ENDPOINT_STATE.CLOSED -> {
          debugIpc("🌼IPC destroy", "$ipcDebugId ${ipc.remote.mmid} $isClosed")
          ipc.doClose()
        }
      }
    }.launchIn(scope)
  }

  /**----- close start*/

  val isClosed get() = ipcLifeCycleState == ENDPOINT_STATE.CLOSED

  abstract suspend fun _doClose()

  // 告知对方，我这条业务线已经准备关闭了
  private suspend fun tryClose() {
    if (ipcLifeCycleState < ENDPOINT_STATE.CLOSING) {
      ipcLifeCycleState = ENDPOINT_STATE.CLOSING
      this.postMessage(EndpointLifecycle(ENDPOINT_STATE.CLOSING))
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
    ipcLifeCycleState = ENDPOINT_STATE.CLOSING
    // 我彻底关闭了
    this.postMessage(EndpointLifecycle.close())
    // 开始触发各类跟ipc绑定的关闭事件
    this.closeSignal.complete(null)
    debugIpc("ipcDestroy=>", " $ipcDebugId 触发完成")
    // 做完全部工作了，关闭
    ipcLifeCycleState = ENDPOINT_STATE.CLOSED
    // 关闭通信信道
    this._doClose()
    scope.cancel()
  }
  /**----- close end*/
}

data class IpcRequestInit(
  var method: PureMethod = PureMethod.GET,
  var body: IPureBody = IPureBody.Empty,
  var headers: PureHeaders = PureHeaders()
)
