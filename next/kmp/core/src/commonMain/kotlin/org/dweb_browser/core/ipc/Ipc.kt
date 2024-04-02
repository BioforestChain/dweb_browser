package org.dweb_browser.core.ipc

import io.ktor.http.Url
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.plus
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.helper.EndpointIpcMessage
import org.dweb_browser.core.ipc.helper.IpcClientRequest
import org.dweb_browser.core.ipc.helper.IpcClientRequest.Companion.toIpc
import org.dweb_browser.core.ipc.helper.IpcError
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcLifecycle
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.core.ipc.helper.IpcRequest
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.ipc.helper.IpcServerRequest
import org.dweb_browser.core.ipc.helper.IpcStream
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.SuspendOnce1
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.withScope
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse

val debugIpc = Debugger("ipc")

open class Ipc private constructor(
  private val pid: Int,
  val endpoint: IpcEndpoint,
  val locale: IMicroModuleManifest,
  val remote: IMicroModuleManifest,
  val pool: IpcPool,
) {
  companion object {
    private var reqId_acc by SafeInt(0)
  }

  constructor(
    endpoint: IpcEndpoint,
    locale: IMicroModuleManifest,
    remote: IMicroModuleManifest,
    pool: IpcPool,
  ) : this(
    pid = pool.generatePid(),
    endpoint = endpoint,
    locale = locale,
    remote = remote,
    pool = pool,
  )

  val scope = endpoint.scope + SupervisorJob()

  val debugId: String = "$pid/${endpoint.debugId}"

  override fun toString() = "Ipc#$debugId"

  private val lifecycleLocaleFlow =
    MutableStateFlow<IpcLifecycle>(IpcLifecycle.Init(pid, locale, remote))
  val onLifecycle =
    lifecycleLocaleFlow.stateIn(scope, SharingStarted.Eagerly, lifecycleLocaleFlow.value)
  val lifecycle get() = lifecycleLocaleFlow.value


  // FIXME 这里两个10应该移除
  private val messageFlow = MutableSharedFlow<IpcMessage>(
    replay = 10,//相当于粘性数据
    extraBufferCapacity = 10,//接受的慢时候，发送的入栈 防止有一个请求挂起的时候 app其他请求无法进行
    onBufferOverflow = BufferOverflow.SUSPEND // 缓冲区溢出的时候挂起 背压
  )
  val onMessage = messageFlow.shareIn(scope, SharingStarted.Eagerly)

  //#region

  private inline fun <T : Any> messagePipeMap(crossinline transform: suspend (value: IpcMessage) -> T?) =
    messageFlow.mapNotNull(transform).shareIn(scope, SharingStarted.Eagerly)

  val onRequest by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap { ipcMessage ->
      when (ipcMessage) {
        is IpcRequest -> when (ipcMessage) {
          is IpcClientRequest -> ipcMessage.toServer(this)
          is IpcServerRequest -> ipcMessage
        }

        else -> null
      }
    }
  }

  val onResponse by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap {
      if (it is IpcResponse) it else null
    }
  }

  val onStream by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap<IpcStream> {
      if (it is IpcStream) it else null
    }
  }

  val onEvent by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap {
      if (it is IpcEvent) it else null
    }
  }


  val onError by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap {
      if (it is IpcError) it else null
    }
  }
  //#endregion

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

  private val _reqResMap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    SafeHashMap<Int, CompletableDeferred<IpcResponse>>().also { reqResMap ->
      onResponse.collectIn(scope) { response ->
        val result = reqResMap.remove(response.reqId)
          ?: throw Exception("no found response by reqId: ${response.reqId}")
        result.complete(response)
      }
    }
  }

  private suspend inline fun buildIpcRequest(url: String, init: IpcRequestInit): IpcRequest {
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
    val ipcRequest = this.buildIpcRequest(url, init)
    return request(ipcRequest)
  }

  private val reqIdSyncObj = SynchronizedObject()
  private fun allocReqId() = synchronized(reqIdSyncObj) { ++reqId_acc }


  /**发送各类消息到remote*/
  suspend fun postMessage(data: IpcMessage) {
    withScope(scope) {
      endpoint.postMessage(EndpointIpcMessage(pid, data))
    }
  }

  suspend inline fun postResponse(reqId: Int, response: PureResponse) {
    postMessage(IpcResponse.fromResponse(reqId, response, this))
  }

  /**分发各类消息到本地*/
  suspend fun dispatchMessage(ipcMessage: IpcMessage) = messageFlow.emit(ipcMessage)

  // 标记ipc通道是否激活
  val isActivity get() = endpoint.isActivity

  suspend fun awaitOpen() = endpoint.awaitOpen()

  // 告知对方我启动了
  suspend fun start() {
    withScope(scope) {
      endpoint.start()
    }
  }

  val isClosed get() = scope.coroutineContext[Job]!!.isCancelled

  /**
   * 等待ipc关闭之后
   *
   * 对比 onBeforeClose ，该函数不在 ipc scope
   */
  suspend fun awaitClosed() = runCatching {
    scope.coroutineContext[Job]!!.join();
    null
  }.getOrElse { it }

  private val beforeClose = MutableSharedFlow<Unit>()

  /**
   * 放置回调函数在ipc关闭之前
   *
   * 这里的 回调函数 被设计成同步，是利用了 close 是 suspend 函数，从而为了避免死循环的引用：
   * 比方说你在多个 ipc 里同时注册了 onBeforeClose 去执行其它 ipc 的 close 函数，如果这里的回调函数是 suspend，那么就会引发循环等待的问题
   * 因此这里强制使用同步函数，目的就是让开发者如要执行什么 suspend 函数，需要显示地使用 scope 去 launch
   */
  val onBeforeClose = beforeClose.shareIn(scope, SharingStarted.Eagerly)

  // 开始触发关闭事件
  suspend fun close(cause: CancellationException? = null) = scope.isActive.trueAlso {
    closeOnce(cause)
  }

  private val closeOnce = SuspendOnce1 { cause: CancellationException? ->
    if (scope.coroutineContext[Job] == coroutineContext[Job]) {
      WARNING("close ipc by self. maybe leak.")
    }
    debugIpc("ipc Close=>", debugId)
    beforeClose.emit(Unit)
    scope.cancel()
  }
  //#region
  /**
   * 在现有的线路中分叉出一个ipc通道
   * 如果自定义了 locale/remote，那么说明自己是帮别人代理
   */
  suspend fun fork(
    locale: IMicroModuleManifest = this.locale,
    remote: IMicroModuleManifest = this.remote,
    autoStart: Boolean = false,
  ) = pool.createIpc(
    endpoint = endpoint,
    locale = locale,
    remote = remote,
    autoStart = autoStart,
  ).also {
    // 自触发
    forkFlow.emit(it)
    // 通知对方
    postMessage(
      IpcLifecycle.Init(
        pid = it.pid,
        /// 对调locale/remote
        locale = it.remote,
        remote = it.locale,
      )
    )
  }

  private val forkFlow = MutableSharedFlow<Ipc>()
  val onFork = forkFlow.shareIn(scope, SharingStarted.Lazily)

  init {
    messageFlow.collectIn(scope) {
      if (it is IpcLifecycle) {
        when (it) {
          is IpcLifecycle.Init -> {
            if (it.pid != pid) {
              forkFlow.emit(
                Ipc(
                  pid = it.pid,
                  endpoint = endpoint,
                  locale = locale,
                  remote = remote,
                  pool = pool,
                )
              )
            }
          }

          else -> lifecycleLocaleFlow.emit(it)
        }
      }
    }

  }
  //#endregion
}

data class IpcRequestInit(
  var method: PureMethod = PureMethod.GET,
  var body: IPureBody = IPureBody.Empty,
  var headers: PureHeaders = PureHeaders()
)
