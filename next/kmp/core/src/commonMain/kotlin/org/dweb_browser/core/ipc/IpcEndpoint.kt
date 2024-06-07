package org.dweb_browser.core.ipc

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import org.dweb_browser.core.ipc.helper.EndpointIpcMessage
import org.dweb_browser.core.ipc.helper.EndpointLifecycle
import org.dweb_browser.core.ipc.helper.EndpointLifecycleClosed
import org.dweb_browser.core.ipc.helper.EndpointLifecycleClosing
import org.dweb_browser.core.ipc.helper.EndpointLifecycleInit
import org.dweb_browser.core.ipc.helper.EndpointLifecycleOpened
import org.dweb_browser.core.ipc.helper.EndpointLifecycleOpening
import org.dweb_browser.core.ipc.helper.EndpointProtocol
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.Producer
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.SafeLinkList
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.SuspendOnce1
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.withScope


/**
 * 通常我们将先进入 opened 状态的称为 endpoint-0，其次是 endpoint-1
 */
abstract class IpcEndpoint {
  private val accPid = atomic(0)

  /**
   * 注册一个Pid
   * endpoint-0 的 ipc-fork 出来的 pid 是偶数
   * endpoint-1 的 ipc-fork 出来的 pid 是奇数
   */
  fun generatePid() = accPid.addAndGet(2)

  internal val forkedIpcMap = SafeHashMap<Int, CompletableDeferred<Ipc>>()

  suspend fun waitForkedIpc(pid: Int): Ipc {
    val ipc = debugEndpoint.timeout(1000, "waitForkedIpc", { "pid=$pid" }) {
      forkedIpcMap.getOrPut(pid) { CompletableDeferred() }.await()
    }
    return ipc
  }


  val debugEndpoint by lazy { Debugger(this.toString()) }
  abstract val debugId: String

  abstract val scope: CoroutineScope

  //#region EndpointIpcMessage
  // 这里的设计相对简单，只需要实现 IO 即可

  /**
   * 发送消息
   */
  abstract suspend fun postIpcMessage(msg: EndpointIpcMessage)

  /**
   * 接收消息
   */

//  abstract val onIpcMessage: SharedFlow<EndpointIpcMessage>

  protected val ipcMessageProducers = SafeHashMap<Int, IpcMessageProducer>()

  inner class IpcMessageProducer(val pid: Int) {
    internal val ipcCompletableDeferred = CompletableDeferred<Ipc>()
    val ipcDeferred: Deferred<Ipc> get() = ipcCompletableDeferred
    val producer = Producer<IpcMessage>("ipc-msg/$debugId/${pid}", scope).apply {
      invokeOnClose { ipcMessageProducers.remove(pid) }
    }
  }

  /**
   * 获取消息管道
   */
  fun getIpcMessageProducer(pid: Int) = ipcMessageProducers.getOrPut(pid) {
    IpcMessageProducer(pid)
  }

  fun getIpcMessageProducer(ipc: Ipc) = getIpcMessageProducer(ipc.pid).apply {
    ipc.onClosed {
      ipcMessageProducers.remove(ipc.pid)
    }
    ipcCompletableDeferred.complete(ipc)
  }
  //#endregion

  //#region EndpointLifecycle
  // 这里的设计相对复杂，因为提供了内置的生命周期相关的实现，包括 握手、关闭
  // 所以这里的 IO 需要通过子类提供的两个 StateFlow 对象来代表

  /**
   * 本地的生命周期状态流
   */
  protected val lifecycleLocaleFlow = MutableStateFlow(EndpointLifecycle(EndpointLifecycleInit))

  /**
   * 远端的生命周期状态流
   */
  protected abstract val lifecycleRemoteFlow: StateFlow<EndpointLifecycle>

  /**
   * 向远端发送 生命周期 信号
   */

  protected abstract suspend fun sendLifecycleToRemote(state: EndpointLifecycle)

  /**
   * 生命周期 监听器
   *
   * > 这里要用 Eagerly，因为是 StateFlow
   */
  val onLifecycle by lazy {
    lifecycleLocaleFlow.stateIn(scope, SharingStarted.Eagerly, lifecycleLocaleFlow.value)
  }

  /**
   * 当前生命周期
   */
  val lifecycle get() = lifecycleLocaleFlow.value

  /**
   * 是否处于可以发送消息的状态
   */
  val isActivity get() = lifecycleLocaleFlow.value.state is EndpointLifecycleOpened

  /**
   * 获取支持的协议，在协商的时候会用到
   */
  protected abstract fun getLocaleSubProtocols(): Set<EndpointProtocol>

  /**
   * 启动生命周期的相关的工作
   */
  abstract suspend fun doStart()

  suspend fun start(await: Boolean = true) {
    withScope(scope) {
      startOnce()
      if (await) {
        awaitOpen("from-start")
      }
    }
  }

  /**
   * 启动
   */
  private val startOnce = SuspendOnce {
    debugEndpoint("start", lifecycle)
    doStart()
    val localeSubProtocols = getLocaleSubProtocols()
    val localSessionId = randomUUID()
    // 当前状态必须是从init开始
    when (val state = lifecycle.state) {
      is EndpointLifecycleInit -> EndpointLifecycle(
        EndpointLifecycleOpening(localeSubProtocols, listOf(localSessionId)),
      ).also {
        sendLifecycleToRemote(it)
        debugEndpoint.verbose("emit-locale-lifecycle", it)
        lifecycleLocaleFlow.emit(it)
      }

      else -> throw IllegalStateException("endpoint state=$state")
    }
    // 监听远端生命周期指令，进行协议协商
    lifecycleRemoteFlow.collectIn(scope) { lifecycleRemote ->
      debugEndpoint.verbose("lifecycle-in", lifecycleRemote)
      when (val remoteState = lifecycleRemote.state) {
        is EndpointLifecycleClosing, is EndpointLifecycleClosed -> close()
        // 收到 opened 了，自己也设置成 opened，代表正式握手成功
        is EndpointLifecycleOpened -> {
          when (val localeState = lifecycleLocaleFlow.value.state) {
            is EndpointLifecycleOpening -> EndpointLifecycle(
              EndpointLifecycleOpened(
                localeState.subProtocols,
                localeState.sessionIds.joinToString("~")
              ).also {
                // 根据 sessionId 来定位 pid 的起点值
                accPid.update { localeState.sessionIds.indexOf(localSessionId) }
              }
            )
              .also {
                sendLifecycleToRemote(it)
                debugEndpoint.verbose("emit-locale-lifecycle", it)
                lifecycleLocaleFlow.emit(it)
              }

            else -> {}
          }
        }
        // 如果对方是 init，代表刚刚初始化，那么发送目前自己的状态
        is EndpointLifecycleInit -> sendLifecycleToRemote(lifecycleLocaleFlow.value)
        // 等收到对方 Opening ，说明对方也开启了，那么开始协商协议，直到一致后才进入 Opened
        is EndpointLifecycleOpening -> {
          when (val localeState = lifecycleLocaleFlow.value.state) {
            is EndpointLifecycleOpening -> {
              val nextState = if (localeState != remoteState) {
                val subProtocols = localeSubProtocols.intersect(remoteState.subProtocols)
                val sessionIds = localeState.sessionIds.union(remoteState.sessionIds)
                  .sortedWith { a, b -> a.compareTo(b) }
                EndpointLifecycle(EndpointLifecycleOpening(subProtocols, sessionIds)).also {
                  debugEndpoint.verbose("emit-locale-lifecycle", it)
                  lifecycleLocaleFlow.emit(it)
                }
              } else {
                EndpointLifecycle(
                  EndpointLifecycleOpened(
                    localeState.subProtocols,
                    localeState.sessionIds.joinToString("~")
                  ).also {
                    // 根据 sessionId 来定位 pid 的起点值
                    accPid.update { localeState.sessionIds.indexOf(localSessionId) }
                  }
                )
              }
              sendLifecycleToRemote(nextState)
            }

            else -> {}
          }
        }
      }
    }
  }

  suspend fun awaitOpen(reason: String? = null) = when (lifecycle.state) {
    is EndpointLifecycleOpened -> lifecycle
    else -> lifecycleLocaleFlow.mapNotNull {
      when (it.state) {
        is EndpointLifecycleOpened -> it
        is EndpointLifecycleClosing, is EndpointLifecycleClosed -> {
          throw IllegalStateException("endpoint already closed")
        }

        else -> null
      }
    }.first().also {
      debugEndpoint("lifecycle-opened", reason)
    }
  }

  suspend fun close(cause: CancellationException? = null) = scope.isActive.trueAlso {
    closeOnce(cause)
  }

  private val closeOnce = SuspendOnce1 { cause: CancellationException? ->
    doClose(cause)
  }

  private val job get() = scope.coroutineContext[Job]!!

  val isClosed get() = job.isCancelled

  suspend fun awaitClosed() = runCatching {
    job.join();
    null
  }.getOrElse { it }

  fun onClosed(handler: CompletionHandler) = job.invokeOnCompletion(handler)

  /**
   * 长任务，需要全部完成才能结束ipcEndpoint
   */
  protected val launchJobs = SafeLinkList<Job>()

  protected open suspend fun doClose(cause: CancellationException? = null) {
    when (lifecycle.state) {
      is EndpointLifecycleOpened, is EndpointLifecycleOpening -> {
        this.sendLifecycleToRemote(EndpointLifecycle(EndpointLifecycleClosing()))
      }

      is EndpointLifecycleClosed -> return
      else -> {}
    }
    beforeClose?.invoke(cause)
    debugEndpoint.timeout(1000, "doClose") {
      // 等待所有长任务完成
      launchJobs.joinAll()
    }
    /// 关闭所有的子通道
    ipcMessageProducers.toList().map { (_, ipcMessageProducer) ->
      ipcMessageProducer.producer.close(cause)
    }
    ipcMessageProducers.clear()
    this.sendLifecycleToRemote(EndpointLifecycle(EndpointLifecycleClosed()))
    scope.cancel(cause)
    afterClosed?.invoke(cause)
  }

  protected var beforeClose: (suspend (cause: CancellationException?) -> Unit)? = null
  protected var afterClosed: ((cause: CancellationException?) -> Unit)? = null
  //#endregion

}