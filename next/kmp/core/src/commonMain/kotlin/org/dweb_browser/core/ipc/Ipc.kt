package org.dweb_browser.core.ipc

import io.ktor.http.Url
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.help.types.MicroModuleManifest
import org.dweb_browser.core.ipc.helper.EndpointIpcMessage
import org.dweb_browser.core.ipc.helper.IpcClientRequest
import org.dweb_browser.core.ipc.helper.IpcError
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcFork
import org.dweb_browser.core.ipc.helper.IpcLifecycle
import org.dweb_browser.core.ipc.helper.IpcLifecycleClosed
import org.dweb_browser.core.ipc.helper.IpcLifecycleClosing
import org.dweb_browser.core.ipc.helper.IpcLifecycleInit
import org.dweb_browser.core.ipc.helper.IpcLifecycleOpened
import org.dweb_browser.core.ipc.helper.IpcLifecycleOpening
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.core.ipc.helper.IpcRequest
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.ipc.helper.IpcServerRequest
import org.dweb_browser.core.ipc.helper.IpcStream
import org.dweb_browser.core.ipc.helper.toIpc
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.DeferredSignal
import org.dweb_browser.helper.Producer
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.SafeLinkList
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.SuspendOnce1
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.globalEmptyScope
import org.dweb_browser.helper.withScope
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse


class Ipc internal constructor(
  val pid: Int,
  val endpoint: IpcEndpoint,
  val locale: MicroModuleManifest,
  val remote: MicroModuleManifest,
  val pool: IpcPool,
  val debugId: String = "${endpoint.debugId}/$pid",
) {
  val debugIpc by lazy { Debugger(this.toString()) }

  companion object {
    private val reqIdAcc = atomic(0)
  }

  private val job = SupervisorJob()
  val scope = endpoint.scope + job

  override fun toString() = "Ipc@$debugId"

  /**
   * 这部分得放最前面，因为有些地方需要立刻使用 onClosed
   */

  //#region close

  private val closeDeferred = CompletableDeferred<CancellationException?>()
  private val closeLock = Mutex(true)

  val isClosed get() = closeDeferred.isCompleted

  /**
   * 等待ipc关闭之后
   *
   * 对比 onBeforeClose ，该函数不在 ipc scope
   *
   * 这里之所以要放在closeLock，是因为 closeDeferred.await 和 invokeOnComplete 是同一个级别的。
   * 因此这里如果直接通过 closeDeferred.await 返回，那么 invokeOnComplete 可能没有执行完成，导致一些预期之外的效果
   */
  suspend fun awaitClosed() = closeLock.withLock { closeDeferred.await() }

  val onClosed = DeferredSignal(closeDeferred)

  // 开始触发关闭事件
  suspend fun close(cause: CancellationException? = null) {
    if (!isClosed) {
      closeOnce(cause)
    }
  }

  fun tryClose(cause: CancellationException? = null) {
    if (scope.isActive) {
      globalEmptyScope.launch(start = CoroutineStart.UNDISPATCHED) {
        close(cause)
      }
    }
  }

  /**
   * 长任务，需要全部完成才能结束ipcEndpoint
   */
  val launchJobs = SafeLinkList<Job>()

  private val closeOnce = SuspendOnce1 { cause: CancellationException? ->
    debugIpc("closing", cause)
    val reason = cause?.message
    IpcLifecycle(IpcLifecycleClosing(reason)).also { closing ->
      lifecycleLocaleFlow.emit(closing)
      sendLifecycleToRemote(closing)
    }
    messageProducer.producer.close(cause)
    closeDeferred.complete(cause)
    closeLock.unlock()
    IpcLifecycle(IpcLifecycleClosed(reason)).also { closed ->
      lifecycleLocaleFlow.emit(closed)
      runCatching { sendLifecycleToRemote(closed) }.getOrNull()
    }
    debugIpc.timeout(1000, "close") {
      launchJobs.joinAll()
    }
    scope.cancel(cause)
    debugIpc("closed", cause)
  }
  //#endregion

  /**
   * 消息生产者，所有的消息在这里分发出去
   */
  private val messageProducer = endpoint.getIpcMessageProducer(this)
  fun onMessage(name: String) = messageProducer.producer.consumer(name)

  //#region 生命周期相关的
  private val lifecycleLocaleFlow = MutableStateFlow(
    IpcLifecycle(IpcLifecycleInit(pid, locale.toCommonAppManifest(), remote.toCommonAppManifest()))
  )

  private val lifecycleRemoteFlow = onMessage("ipc-lifecycle-remote#$pid").mapNotNull { event ->
    event.consumeAs<IpcLifecycle>()
  }

  val lifecycle get() = lifecycleLocaleFlow.value
  val onLifecycle = lifecycleLocaleFlow.stateIn(scope, SharingStarted.Eagerly, lifecycle)


  // 标记ipc通道是否激活
  val isActivity get() = endpoint.isActivity

  suspend fun awaitOpen(reason: String? = null) = when (val state = lifecycle.state) {
    is IpcLifecycleOpened -> lifecycle
    else -> lifecycleLocaleFlow.mapNotNull {
      debugIpc("awaitOpen", "state=$it reason=$reason")
      val ipcLifecycle = when (it.state) {
        is IpcLifecycleOpened -> it
        is IpcLifecycleClosing, is IpcLifecycleClosed -> {
          throw IllegalStateException("ipc already closed")
        }

        else -> null
      }
      ipcLifecycle
    }.first().also {
      debugIpc("lifecycle-opened", reason)
    }
  }

  /**
   * 启动，会至少等到endpoint握手完成
   */
  suspend fun start(await: Boolean = true, reason: String = "") {
    if (await) {
      withScope(scope) {
        debugIpc.verbose("start-begin", reason)
        endpoint.start(true)
        startOnce()
        awaitOpen("from-start $reason")
      }
    } else {
      scope.launch {
        debugIpc.verbose("start-begin", reason)
        endpoint.start(true)
        startOnce()
      }
    }
  }

  private val startOnce = SuspendOnce {
    debugIpc("start", lifecycle)
    // 当前状态必须是从init开始
    when (val state = lifecycle.state) {
      // 告知对方我启动了
      is IpcLifecycleInit -> IpcLifecycle(IpcLifecycleOpening).also {
        sendLifecycleToRemote(it)
        debugIpc.verbose("emit-locale-lifecycle", it)
        lifecycleLocaleFlow.emit(it)
      }

      else -> throw IllegalStateException("fail to start: ipc=$this state=$state")
    }
    // 监听远端生命周期指令，进行协议协商
    lifecycleRemoteFlow.collectIn(scope) { lifecycleRemote ->
      debugIpc.verbose("lifecycle-in") { "remote=$lifecycleRemote locale=$lifecycle" }
      val doIpcOpened = suspend {
        IpcLifecycle(IpcLifecycleOpened).also {
          debugIpc.verbose("emit-locale-lifecycle", it)
          sendLifecycleToRemote(it)
          lifecycleLocaleFlow.emit(it)
        }
      }
      when (lifecycleRemote.state) {
        is IpcLifecycleClosing, is IpcLifecycleClosed -> tryClose(CancellationException("lifecycle close"))
        // 收到 opened 了，自己也设置成 opened，代表正式握手成功
        is IpcLifecycleOpened -> {
          when (lifecycle.state) {
            is IpcLifecycleOpening -> doIpcOpened()
            else -> {}
          }
        }
        // 如果对方是 init，代表刚刚初始化，那么发送目前自己的状态
        is IpcLifecycleInit -> sendLifecycleToRemote(lifecycle)
        // 等收到对方 Opening ，说明对方也开启了，那么开始协商协议，直到一致后才进入 Opened
        is IpcLifecycleOpening -> doIpcOpened()
      }
    }
    // 监听并分发 所有的消息
    onMessage("fork#$debugId").collectIn(scope) { event ->
      event.consumeAs<IpcFork> { ipcFork ->
        val forkedIpc = Ipc(
          pid = ipcFork.pid,
          endpoint = endpoint,
          locale = locale,
          remote = remote,
          pool = pool,
        )
        pool.safeCreatedIpc(
          forkedIpc, autoStart = ipcFork.autoStart, startReason = ipcFork.startReason
        )
        endpoint.forkedIpcMap.getOrPut(forkedIpc.pid) { CompletableDeferred() }.complete(forkedIpc)
        forkProducer.send(forkedIpc)
      }
    }
  }

  /**
   * 向远端发送 生命周期 信号
   */
  private suspend fun sendLifecycleToRemote(state: IpcLifecycle) {
    debugIpc.verbose("lifecycle-out", state)
    endpoint.postIpcMessage(EndpointIpcMessage(pid, state))
  }

  val waitForkedIpc = endpoint::waitForkedIpc

  /**
   * 在现有的线路中分叉出一个ipc通道
   * 如果自定义了 locale/remote，那么说明自己是帮别人代理
   */
  suspend fun fork(
    locale: MicroModuleManifest = this.locale,
    remote: MicroModuleManifest = this.remote,
    autoStart: Boolean = false,
    startReason: String? = null,
    pid: Int? = null,
  ): Ipc {
    awaitOpen("then-fork")
    // 这里确保 endpoint opened 了再 generatePid
    val ipcId = pid ?: endpoint.generatePid()
    val forkedIpc = pool.createIpc(
      pid = ipcId,
      endpoint = endpoint,
      locale = locale,
      remote = remote,
      autoStart = autoStart,
      startReason = startReason
    )
    endpoint.forkedIpcMap.getOrPut(forkedIpc.pid) { CompletableDeferred() }.complete(forkedIpc)
    // 自触发
    forkProducer.send(forkedIpc)
    // 通知对方
    postMessage(
      IpcFork(
        pid = forkedIpc.pid,
        autoStart = autoStart,
        startReason = startReason,
        /// 对调locale/remote
        locale = forkedIpc.remote.toCommonAppManifest(),
        remote = forkedIpc.locale.toCommonAppManifest(),
      )
    )
    return forkedIpc
  }

  private val forkProducer = Producer<Ipc>("fork#$debugId", scope)
  fun onFork(name: String) = forkProducer.consumer(name)

  //#endregion

  //#region 消息相关的


  private inline fun <T : Any> messagePipeMap(
    name: String,
    crossinline mapNotNull: suspend (value: IpcMessage) -> T?,
  ) = Producer<T>("$debugId/$name", scope).also { producer ->
    onClosed { cause ->
      producer.close(cause.exceptionOrNull())
    }
    onMessage(name).collectIn(scope) { event ->
      event.consumeMapNotNull {
        mapNotNull(it)
      }?.also {
        producer.send(it, event.order)
      }
    }
  }

  private val requestProducer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap("request") { ipcMessage ->
      when (ipcMessage) {
        is IpcRequest -> when (ipcMessage) {
          is IpcClientRequest -> ipcMessage.toServer(this)
          is IpcServerRequest -> ipcMessage
        }

        else -> null
      }
    }
  }

  fun onRequest(name: String) = requestProducer.consumer(name)

  private val responseProducer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap("response") {
      if (it is IpcResponse) it else null
    }
  }

  fun onResponse(name: String) = responseProducer.consumer(name)

  private val streamProducer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap("stream") {
      if (it is IpcStream) it as IpcStream else null
    }
  }

  fun onStream(name: String) = streamProducer.consumer(name)

  private val eventProducer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap("event") {
      if (it is IpcEvent) it else null
    }
  }

  fun onEvent(name: String) = eventProducer.consumer(name)


  private val errorProducer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    messagePipeMap("error") {
      if (it is IpcError) it else null
    }
  }

  fun onError(name: String) = errorProducer.consumer(name)

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
      onResponse("req-res").collectIn(scope) { event ->
        // 消耗掉
        val response = event.consume()
        val result = reqResMap.remove(response.reqId) ?: return@collectIn debugIpc(
          "reqResMap",
          "onResponse",
          "no found response [${event.data}]  by reqId: ${event.data.reqId}"
        )
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
    return request(
      pureRequest.toIpc(allocReqId(), this)
    ).toPure()
  }

  suspend fun request(url: String, init: IpcRequestInit): IpcResponse {
    val ipcRequest = this.buildIpcRequest(url, init)
    return request(ipcRequest)
  }

  private fun allocReqId() = reqIdAcc.incrementAndGet()


  /**发送各类消息到remote*/
  suspend fun postMessage(data: IpcMessage) {
    runCatching {
      awaitOpen("then-postMessage")
      withScope(scope) {
        endpoint.postIpcMessage(EndpointIpcMessage(pid, data))
      }
    }.getOrElse {
      WARNING("fail to postMessage: $data")
      WARNING(it.message)
    }
  }

  suspend inline fun postResponse(reqId: Int, response: PureResponse) {
    postMessage(IpcResponse.fromResponse(reqId, response, this))
  }
  //#endregion

  init {
    endpoint.onClosed {
      tryClose(CancellationException("endpoint closed", it))
    }
  }
}

data class IpcRequestInit(
  var method: PureMethod = PureMethod.GET,
  var body: IPureBody = IPureBody.Empty,
  var headers: PureHeaders = PureHeaders(),
)
