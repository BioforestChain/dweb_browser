package org.dweb_browser.core.ipc

import io.ktor.http.Url
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
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
import org.dweb_browser.helper.SuspendOnce
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
  private val lifecycleRemoteFlow =
    MutableStateFlow<IpcLifecycle>(IpcLifecycle.Init(pid, remote, locale))
  val onLifecycle =
    lifecycleLocaleFlow.stateIn(scope, SharingStarted.Eagerly, lifecycleLocaleFlow.value)
  val lifecycle get() = lifecycleLocaleFlow.value


  private val messageFlow = endpoint.onMessage.mapNotNull {
    if (it.pid == pid) it.ipcMessage else null
  }

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
    awaitOpen()
    withScope(scope) {
      endpoint.postMessage(EndpointIpcMessage(pid, data))
    }
  }

  suspend inline fun postResponse(reqId: Int, response: PureResponse) {
    postMessage(IpcResponse.fromResponse(reqId, response, this))
  }

  // 标记ipc通道是否激活
  val isActivity get() = endpoint.isActivity

  suspend fun awaitOpen() = lifecycleRemoteFlow.mapNotNull { state ->
    when (state) {
      is IpcLifecycle.Opened -> state
      is IpcLifecycle.Closing, is IpcLifecycle.Closed -> {
        throw IllegalStateException("ipc already closed")
      }

      else -> null
    }
  }.first().also {
    debugIpc("awaitOpen", it)
  }

  /**
   * 启动，会至少等到endpoint握手完成
   */
  suspend fun start(await: Boolean = true) {
    withScope(scope) {
      endpoint.start(true)
      startOnce()
      if (await) {
        awaitOpen()
      }
    }
  }

  private val startOnce = SuspendOnce {
    // 当前状态必须是从init开始
    when (val state = lifecycle) {
      // 告知对方我启动了
      is IpcLifecycle.Init -> sendLocaleLifecycle(IpcLifecycle.Opening())

      else -> throw IllegalStateException("endpoint state=$state")
    }
    debugIpc("start", this@Ipc)
    // 监听远端生命周期指令，进行协议协商
    lifecycleRemoteFlow.collectIn(scope) { state ->
      debugIpc("lifecycle-in") { "${this@Ipc} << $state" }
      when (state) {
        is IpcLifecycle.Closing, is IpcLifecycle.Closed -> close()
        // 收到 opened 了，自己也设置成 opened，代表正式握手成功
        is IpcLifecycle.Opened -> {
          when (lifecycleLocaleFlow.value) {
            is IpcLifecycle.Opening -> lifecycleLocaleFlow.emit(IpcLifecycle.Opened())

            else -> {}
          }
        }
        // 如果对方是 init，代表刚刚初始化，那么发送目前自己的状态
        is IpcLifecycle.Init -> sendLocaleLifecycle(lifecycleLocaleFlow.value)
        // 等收到对方 Opening ，说明对方也开启了，那么开始协商协议，直到一致后才进入 Opened
        is IpcLifecycle.Opening -> {
          sendLocaleLifecycle(IpcLifecycle.Opened())
        }
      }
    }
    // 监听并分发 所有的消息
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

          else -> lifecycleRemoteFlow.emit(it)
        }
      }
    }
  }

  /**
   * 更新并发送 本地生命周期状态
   */
  private suspend fun sendLocaleLifecycle(state: IpcLifecycle) {
    debugIpc("lifecycle-out") { "$this >> $state " }
    endpoint.postMessage(EndpointIpcMessage(pid, state))
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
  ).also { forkedIpc ->
    // 自触发
    forkFlow.emit(forkedIpc)
    // 通知对方
    postMessage(
      IpcLifecycle.Init(
        pid = forkedIpc.pid,
        /// 对调locale/remote
        locale = forkedIpc.remote,
        remote = forkedIpc.locale,
      )
    )
  }

  private val forkFlow = MutableSharedFlow<Ipc>()
  val onFork = forkFlow.run {
    if (debugIpc.isEnable) {
      onEach { debugIpc("onFork", it) }
    } else this
  }.shareIn(scope, SharingStarted.Lazily)


//#endregion
}

data class IpcRequestInit(
  var method: PureMethod = PureMethod.GET,
  var body: IPureBody = IPureBody.Empty,
  var headers: PureHeaders = PureHeaders()
)
