package org.dweb_browser.core.ipc

import io.ktor.http.Url
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.helper.EndpointIpcMessage
import org.dweb_browser.core.ipc.helper.IpcClientRequest
import org.dweb_browser.core.ipc.helper.IpcError
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcFork
import org.dweb_browser.core.ipc.helper.IpcLifecycle
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
import org.dweb_browser.helper.asProducer
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.withScope
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse


open class Ipc internal constructor(
  val pid: Int,
  val endpoint: IpcEndpoint,
  val locale: IMicroModuleManifest,
  val remote: IMicroModuleManifest,
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


  //  val onMessage by lazy { endpoint.getIpcMessageChannel(pid).observe() }
  private val messageProducer = endpoint.getIpcMessageProducer(pid)
  fun onMessage(name: String) = messageProducer.consumer(name)

  //#region 生命周期相关的
  private val lifecycleLocaleFlow = MutableStateFlow<IpcLifecycle>(
    IpcLifecycle.Init(pid, locale.toCommonAppManifest(), remote.toCommonAppManifest())
  )

  private val lifecycleRemoteFlow = onMessage("ipc-lifecycle-remote#$pid").mapNotNull { event ->
    event.consumeAs<IpcLifecycle>()
  }

  val lifecycle get() = lifecycleLocaleFlow.value
  val onLifecycle = lifecycleLocaleFlow.stateIn(scope, SharingStarted.Eagerly, lifecycle)


  // 标记ipc通道是否激活
  val isActivity get() = endpoint.isActivity

  suspend fun awaitOpen(reason: String? = null) = when (val state = lifecycle) {
    is IpcLifecycle.IpcOpened -> state
    else -> lifecycleLocaleFlow.mapNotNull {
      debugIpc("awaitOpen", "state=$it reason=$reason")
      when (it) {
        is IpcLifecycle.IpcOpened -> it
        is IpcLifecycle.IpcClosing, is IpcLifecycle.IpcClosed -> {
          throw IllegalStateException("ipc already closed")
        }

        else -> null
      }
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
        endpoint.start(true)
        startOnce()
        awaitOpen("from-start $reason")
      }
    } else {
      scope.launch {
        endpoint.start(true)
        startOnce()
      }
    }
  }

  private val startOnce = SuspendOnce {
    debugIpc("start", lifecycle)
    // 当前状态必须是从init开始
    when (val state = lifecycle) {
      // 告知对方我启动了
      is IpcLifecycle.Init -> IpcLifecycle.IpcOpening().also {
        sendLifecycleToRemote(it)
        debugIpc("emit-locale-lifecycle", it)
        lifecycleLocaleFlow.emit(it)
      }

      else -> throw IllegalStateException("fail to start: ipc=$this state=$state")
    }
    // 监听远端生命周期指令，进行协议协商
    lifecycleRemoteFlow.collectIn(scope) { state ->
      debugIpc("lifecycle-in", state)
      when (state) {
        is IpcLifecycle.IpcClosing, is IpcLifecycle.IpcClosed -> scope.launch(start = CoroutineStart.UNDISPATCHED) { close() }
        // 收到 opened 了，自己也设置成 opened，代表正式握手成功
        is IpcLifecycle.IpcOpened -> {
          when (lifecycle) {
            is IpcLifecycle.IpcOpening -> IpcLifecycle.IpcOpened().also {
              debugIpc("emit-locale-lifecycle", it)
              sendLifecycleToRemote(it)
              lifecycleLocaleFlow.emit(it)
            }

            else -> {}
          }
        }
        // 如果对方是 init，代表刚刚初始化，那么发送目前自己的状态
        is IpcLifecycle.Init -> sendLifecycleToRemote(lifecycle)
        // 等收到对方 Opening ，说明对方也开启了，那么开始协商协议，直到一致后才进入 Opened
        is IpcLifecycle.IpcOpening -> {
          sendLifecycleToRemote(IpcLifecycle.IpcOpened())
        }
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
        forkedIpcLock.withLock {
          forkedIpcMap[forkedIpc.pid] = forkedIpc
          forkProducer.send(forkedIpc)
        }
      }
    }
  }

  /**
   * 向远端发送 生命周期 信号
   */
  private suspend fun sendLifecycleToRemote(state: IpcLifecycle) {
    debugIpc("lifecycle-out", state)
    endpoint.postIpcMessage(EndpointIpcMessage(pid, state))
  }


  private val closeDeferred = CompletableDeferred<CancellationException?>()

  val isClosed get() = closeDeferred.isCompleted

  /**
   * 等待ipc关闭之后
   *
   * 对比 onBeforeClose ，该函数不在 ipc scope
   */
  suspend fun awaitClosed() = closeDeferred.await()

  val onClosed = DeferredSignal(closeDeferred)

  // 开始触发关闭事件
  suspend fun close(cause: CancellationException? = null) = scope.isActive.trueAlso {
    closeOnce(cause)
  }

  /**
   * 长任务，需要全部完成才能结束ipcEndpoint
   */
  val launchJobs = SafeLinkList<Job>()

  private val closeOnce = SuspendOnce1 { cause: CancellationException? ->
    if (scope.coroutineContext[Job] == coroutineContext[Job]) {
      WARNING("close ipc by self. maybe leak.")
    }
    debugIpc("closing", cause)
    val reason = cause?.message
    sendLifecycleToRemote(IpcLifecycle.IpcClosing(reason))
    messageProducer.closeWrite(cause)
    launchJobs.joinAll()
    closeDeferred.complete(cause)
    IpcLifecycle.IpcClosed(reason).also { closed ->
      lifecycleLocaleFlow.emit(closed)
      sendLifecycleToRemote(closed)
    }
    messageProducer.close(cause)
    scope.cancel(cause)
    debugIpc("closed", cause)
  }

  /**
   * 这里lock用来将 forkedIpcMap.set 和 forkProducer.emit 做成一个原子操作
   */
  private val forkedIpcLock = Mutex()
  private val forkedIpcMap = mutableMapOf<Int, Ipc>()
  fun getForkedIpc(id: Int) = forkedIpcMap[id]

  suspend fun waitForkedIpc(pid: Int): Ipc {
    return coroutineScope {
      forkedIpcLock.withLock {
        /// 因为 forkedIpcMap.set 和 forkProducer.emit 是一个原子操作，所以在lock中，如果找不到，可以开始一个监听
        forkedIpcMap[pid]?.let { CompletableDeferred(it) }
          ?: async(start = CoroutineStart.UNDISPATCHED) {
            onFork("waitForkedIpc:$pid").filter { it.data.pid == pid }.first().data
          }
      }
    }.await()
  }

  /**
   * 在现有的线路中分叉出一个ipc通道
   * 如果自定义了 locale/remote，那么说明自己是帮别人代理
   */
  suspend fun fork(
    locale: IMicroModuleManifest = this.locale,
    remote: IMicroModuleManifest = this.remote,
    autoStart: Boolean = false,
    startReason: String? = null,
  ): Ipc {
    awaitOpen("then-fork")
    val forkedIpc = pool.createIpc(
      pid = pool.generatePid(),
      endpoint = endpoint,
      locale = locale,
      remote = remote,
      autoStart = autoStart,
      startReason = startReason
    )
    forkedIpcLock.withLock {
      forkedIpcMap[forkedIpc.pid] = forkedIpc
      // 自触发
      forkProducer.send(forkedIpc)
    }
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

  /**
   * 因为要实现 自触发，所以这里使用 Mutable
   */
  private val forkProducer = Producer<Ipc>("fork#$debugId", scope)
  fun onFork(name: String) = forkProducer.consumer(name)

  //#endregion

  //#region 消息相关的


  private inline fun <T : Any> messagePipeMap(
    name: String,
    crossinline mapNoNull: suspend (value: IpcMessage) -> T?,
  ) = onMessage(name).mapNotNull { event ->
    event.next();
    mapNoNull(event.data)?.also { event.consume() }
  }.asProducer(messageProducer.name + "." + name, scope)

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

  private fun onResponse(name: String) = responseProducer.consumer(name)

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
          "reqResMap", "onResponse", "no found response by reqId: ${event.data.reqId}"
        )
        result.complete(event.consume())
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
    awaitOpen("then-postMessage")
    withScope(scope) {
      endpoint.postIpcMessage(EndpointIpcMessage(pid, data))
    }
  }

  suspend inline fun postResponse(reqId: Int, response: PureResponse) {
    postMessage(IpcResponse.fromResponse(reqId, response, this))
  }
  //#endregion

}

data class IpcRequestInit(
  var method: PureMethod = PureMethod.GET,
  var body: IPureBody = IPureBody.Empty,
  var headers: PureHeaders = PureHeaders(),
)
