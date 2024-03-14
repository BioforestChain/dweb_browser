package org.dweb_browser.core.module

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.help.types.CommonAppManifest
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.help.types.MicroModuleManifest
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.std.permission.PermissionProvider
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.pure.http.PureRequest

typealias Router = MutableMap<String, AppRun>
typealias AppRun = (options: NativeOptions) -> Any
typealias NativeOptions = MutableMap<String, String>

enum class MMState {
  BOOTSTRAP, SHUTDOWN,
}

val debugMicroModule = Debugger("MicroModule")

abstract class MicroModule(val manifest: MicroModuleManifest) : IMicroModuleManifest by manifest {
  companion object {}

  open val routers: Router? = null

  private var runningStateLock = StatePromiseOut.resolve(MMState.SHUTDOWN)
  private val readyLock = Mutex(true)

  private fun getModuleCoroutineScope() =
    CoroutineScope(SupervisorJob() + ioAsyncExceptionHandler + CoroutineName(mmid))

  private var _scope: CoroutineScope = getModuleCoroutineScope()
  val ioAsyncScope get() = _scope

  val running get() = runningStateLock.value == MMState.BOOTSTRAP

  protected open suspend fun beforeBootstrap(bootstrapContext: BootstrapContext): Boolean {
    if (this.runningStateLock.state == MMState.BOOTSTRAP) {
      debugMicroModule("module ${this.mmid} already running");
      return false
    }
    this.runningStateLock.waitPromise() // 确保已经完成上一个状态
    this.runningStateLock = StatePromiseOut(MMState.BOOTSTRAP)
    this._bootstrapContext = bootstrapContext // 保存context
    // 创建一个新的
    if (!_scope.isActive) {
      _scope = getModuleCoroutineScope()
    }
    return true
  }

  /**
   * 获取权限提供器，这需要在bootstrap之前就能提供
   * 因为 dweb_permissions 字段并不难直接使用，所以需要模块对其数据进行加工处理，从而确保数据合法与安全
   */
  abstract suspend fun getSafeDwebPermissionProviders(): List<PermissionProvider>

  private var _bootstrapContext: BootstrapContext? = null
  val bootstrapContext get() = _bootstrapContext ?: throw Exception("module no run.")

  protected abstract suspend fun _bootstrap(bootstrapContext: BootstrapContext)
  private suspend fun afterBootstrap(bootstrapContext: BootstrapContext) {
    debugMicroModule("afterBootstrap", "ready: $mmid, ${_ipcSet.size}")
    onConnect { (ipc) ->
      ipc.awaitStart()
    }
    // 等待mm连接池中的ipc都连接完成
    for (ipc in _ipcSet) {
      ipc.awaitStart()
    }
    this.runningStateLock.resolve()
    readyLock.unlock()
  }


  private val lifecycleLock = Mutex()

  suspend fun bootstrap(bootstrapContext: BootstrapContext) = lifecycleLock.withLock {
    if (this.beforeBootstrap(bootstrapContext)) {
      try {
        this._bootstrap(bootstrapContext);
      } finally {
        this.afterBootstrap(bootstrapContext);
      }
    }
  }

  protected open suspend fun beforeShutdown(): Boolean {
    if (this.runningStateLock.state == MMState.SHUTDOWN) {
      debugMicroModule("module $mmid already shutdown");
      return false
    }
    readyLock.lock()
    this.runningStateLock.waitPromise() // 确保已经完成上一个状态
    this.runningStateLock = StatePromiseOut(MMState.SHUTDOWN)

    /// 关闭所有的通讯
    _ipcSet.toList().forEach {
      it.close()
    }
    _ipcSet.clear()
    return true
  }

  /**
   * 让回调函数一定在启动状态内被运行
   */
  suspend fun <R> withBootstrap(block: suspend () -> R) = readyLock.withLock { block() }

  protected abstract suspend fun _shutdown()
  protected open suspend fun afterShutdown() {
    _afterShutdownSignal.emitAndClear()
    _connectSignal.clear()
    runningStateLock.resolve()
    this._bootstrapContext = null
    // 取消所有的工作
    this.ioAsyncScope.cancel()
  }

  suspend fun shutdown() = lifecycleLock.withLock {
    if (this.beforeShutdown()) {
      try {
        this._shutdown()
      } finally {
        this.afterShutdown()
      }
    }
  }

  suspend fun dispose() {
    this._dispose()
  }

  protected open suspend fun _dispose() {
  }

  private val _afterShutdownSignal = SimpleSignal();
  val onAfterShutdown = _afterShutdownSignal.toListener()

  /**
   * MicroModule连接池
   */
  private val _ipcSet = mutableSetOf<Ipc>();

  suspend fun addToIpcSet(ipc: Ipc): Boolean {
    debugMicroModule(
      "addToIpcSet",
      "「${ipc.channelId}」 $mmid => ${ipc.remote.mmid} , ${runningStateLock.isResolved}:${runningStateLock.value}"
    )
    if (runningStateLock.isResolved && runningStateLock.value == MMState.BOOTSTRAP) {
      ipc.awaitStart()
      debugMicroModule("addToIpcSet", "✅ ${ipc.channelId} end")
    }
    return if (this._ipcSet.add(ipc)) {
      ipc.onClose {
        _ipcSet.remove(ipc)
      }
      true
    } else false
  }

  /**
   * 内部程序与外部程序通讯的方法
   * TODO 这里应该是可以是多个
   */
  private val _connectSignal = Signal<IpcConnectArgs>();

  /**
   * 给内部程序自己使用的 onConnect，外部与内部建立连接时使用
   * 因为 NativeMicroModule 的内部程序在这里编写代码，所以这里会提供 onConnect 方法
   * 如果时 JsMicroModule 这个 onConnect 就是写在 WebWorker 那边了
   */
  val onConnect = _connectSignal.toListener()

  /**
   * 尝试连接到指定对象
   */
  suspend fun connect(mmid: MMID, reason: PureRequest? = null): Ipc {
    val (ipc) = this.bootstrapContext.dns.connect(mmid, reason)
    return ipc
  }

  /**
   * 收到一个连接，触发相关事件
   */
  suspend fun beConnect(ipc: Ipc, reason: PureRequest) {
    if (this.addToIpcSet(ipc)) {
      _connectSignal.emit(Pair(ipc, reason))
    }
  }

  override fun toString(): String {
    return "MicroModule($mmid)"
  }

  open fun toManifest(): CommonAppManifest {
    return manifest.toCommonAppManifest()
  }
}

typealias IpcConnectArgs = Pair<Ipc, PureRequest>

class StatePromiseOut<T>(val state: T) : PromiseOut<T>() {
  companion object {
    fun <T> resolve(state: T) = StatePromiseOut(state).also { it.resolve() }
  }

  fun resolve() {
    super.resolve(state)
  }
}