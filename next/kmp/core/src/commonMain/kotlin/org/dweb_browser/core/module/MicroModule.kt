package org.dweb_browser.core.module

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.help.types.CommonAppManifest
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.help.types.MicroModuleManifest
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.std.permission.PermissionProvider
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.Producer
import org.dweb_browser.helper.ReasonLock
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.SafeHashSet
import org.dweb_browser.helper.defaultAsyncExceptionHandler
import org.dweb_browser.helper.listen
import org.dweb_browser.pure.http.PureRequest
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

typealias Router = MutableMap<String, AppRun>
typealias AppRun = (options: NativeOptions) -> Any
typealias NativeOptions = MutableMap<String, String>

enum class MMState {
  BOOTSTRAP, SHUTDOWN,
}

abstract class MicroModule(val manifest: MicroModuleManifest) : IMicroModuleManifest by manifest {
  val debugMM by lazy { Debugger("$this") }

  companion object {}

  /**
   * 获取权限提供器，这需要在bootstrap之前就能提供
   * 因为 dweb_permissions 字段并不难直接使用，所以需要模块对其数据进行加工处理，从而确保数据合法与安全
   */
  abstract suspend fun getSafeDwebPermissionProviders(): List<PermissionProvider>

  /**
   * 如果启动
   * 那么会创建该运行时
   */
  abstract inner class Runtime : IMicroModuleManifest by manifest {
    val debugMM get() = this@MicroModule.debugMM
    abstract val bootstrapContext: BootstrapContext

    open val routers: Router? = null

    protected val mmScope =
      CoroutineScope(SupervisorJob() + defaultAsyncExceptionHandler + CoroutineName(manifest.mmid))

    fun getRuntimeScope() = mmScope

    private inner class ScopeJob(val job: Job, val cancelable: Boolean) {
      init {
        jobs.add(this)
        job.invokeOnCompletion {
          jobs.remove(this)
        }
      }
    }

    private val jobs = mutableSetOf<ScopeJob>()

    /**
     * 使用当前的MicroModule的生命周期来启动一个job
     * 生命周期会确保这个job完成后才会完全结束
     */
    fun scopeLaunch(
      context: CoroutineContext = EmptyCoroutineContext,
      cancelable: Boolean,
      action: suspend CoroutineScope.() -> Unit,
    ) = mmScope.launch(context = context, block = action).also { job ->
      ScopeJob(job, cancelable)
    }

    fun <R> scopeAsync(
      context: CoroutineContext = EmptyCoroutineContext,
      cancelable: Boolean,
      action: suspend CoroutineScope.() -> R,
    ) = mmScope.async(context = context, block = action).also { job ->
      ScopeJob(job, cancelable)
    }

    private val stateLock = Mutex()
    private var state = MMState.SHUTDOWN
    val isRunning get() = state == MMState.BOOTSTRAP


    protected abstract suspend fun _bootstrap()

    suspend fun bootstrap() = stateLock.withLock {
      if (state != MMState.BOOTSTRAP) {
        debugMM("bootstrap-start")
        _bootstrap();
        debugMM("bootstrap-end")
      } else {
        debugMM("bootstrap", "$mmid already running")
      }
      state = MMState.BOOTSTRAP
    }

    val microModule get() = this@MicroModule

    private val beforeShutdownFlow = MutableSharedFlow<Unit>()
    val onBeforeShutdown = beforeShutdownFlow.shareIn(mmScope, SharingStarted.Eagerly)

    private val shutdownDeferred = CompletableDeferred<Unit>()
    val awaitShutdown = shutdownDeferred::await
    fun onShutdown(action: () -> Unit) {
      shutdownDeferred.invokeOnCompletion { action() }
    }

//
//    /**
//     * 让回调函数一定在启动状态内被运行
//     */
//    suspend fun <R> withBootstrap(block: suspend () -> R) = readyLock.withLock { block() }

    protected abstract suspend fun _shutdown()

    suspend fun shutdown() = stateLock.withLock {
      if (state != MMState.SHUTDOWN) {
        debugMM("shutdown-start")
        debugMM("shutdown-before-start")
        beforeShutdownFlow.emit(Unit)
        debugMM("shutdown-before-end")
        _shutdown()
        shutdownDeferred.complete(Unit)
        debugMM("shutdown-end")
        // 等待注册的任务完成
        while (jobs.isNotEmpty()) {
          val sj = jobs.firstOrNull() ?: continue
          if (sj.cancelable) {
            sj.job.cancel()
          } else {
            sj.job.join()
          }
          jobs.remove(sj)
        }
        // 取消所有的工作
        mmScope.cancel()
      }
      state = MMState.SHUTDOWN
    }

    /**
     * MicroModule 引用池
     */
    private val connectionLinks = SafeHashSet<Ipc>()


    /**
     * 内部程序与外部程序通讯的方法
     */
    private val ipcConnectedProducer = Producer<IpcConnectArgs>("ipcConnect", mmScope);

    /**
     * 给内部程序自己使用的 onConnect，外部与内部建立连接时使用
     * 因为 NativeMicroModule 的内部程序在这里编写代码，所以这里会提供 onConnect 方法
     * 如果时 JsMicroModule 这个 onConnect 就是写在 WebWorker 那边了
     */
    val onConnect = ipcConnectedProducer.consumer("for-internal")

    private val connectReason = ReasonLock()
    private val connectionMap = SafeHashMap<MMID, Ipc>()

    /**
     * 尝试连接到指定对象
     */
    suspend fun connect(mmid: MMID, reason: PureRequest? = null) = connectReason.withLock(mmid) {
      debugMM("connect", mmid)
      connectionMap[mmid] ?: bootstrapContext.dns.connect(mmid, reason).also {
        connectionMap[mmid] = it
        beConnect(it, reason)
      }
    }

    /**
     * 收到一个连接，触发相关事件
     */
    suspend fun beConnect(ipc: Ipc, reason: PureRequest?) {
      scopeLaunch(cancelable = false) {
        if (connectionLinks.add(ipc)) {
          debugMM("beConnect", ipc)
          // 这个ipc分叉出来的ipc也会一并归入管理
          ipc.onFork("beConnect").listen {
            ipc.debugIpc("onFork", it.data)
            beConnect(it.consume(), null)
          }
          onBeforeShutdown.listen {
            ipc.close()
          }
          ipc.onClosed {
            connectionLinks.remove(ipc)
          }
          // 尝试保存到双向连接索引中
          ipc.remote.mmid.also { remoteMmid ->
            connectReason.withLock(remoteMmid) {
              if (!connectionMap.contains(remoteMmid)) {
                connectionMap[remoteMmid] = ipc
              }
            }
          }
          ipcConnectedProducer.send(IpcConnectArgs(ipc, reason))
        }
      }
    }
  }

  private val runtimeLock = Mutex()
  var runtimeOrNull: Runtime? = null
    private set
  val isRunning get() = runtimeOrNull != null
  open val runtime get() = runtimeOrNull ?: throw IllegalStateException("$this is no running")
  suspend fun bootstrap(bootstrapContext: BootstrapContext) = runtimeLock.withLock {
    runtimeOrNull ?: createRuntime(bootstrapContext).also {
      runtimeOrNull = it
      it.onShutdown {
        runtimeOrNull = null
      }
      it.bootstrap()
    }
  }

  abstract fun createRuntime(bootstrapContext: BootstrapContext): Runtime

  override fun toString(): String {
    return "MM($mmid)"
  }

  open fun toManifest(): CommonAppManifest {
    return manifest.toCommonAppManifest()
  }
}

data class IpcConnectArgs(val ipc: Ipc, val reason: PureRequest?)
