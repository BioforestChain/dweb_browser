package org.dweb_browser.microservice.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Callback
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.microservice.help.types.CommonAppManifest
import org.dweb_browser.microservice.help.types.IMicroModuleManifest
import org.dweb_browser.microservice.help.types.MMID
import org.dweb_browser.microservice.help.types.MicroModuleManifest
import org.dweb_browser.microservice.http.PureRequest
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.IpcEvent

typealias Router = MutableMap<String, AppRun>
typealias AppRun = (options: NativeOptions) -> Any
typealias NativeOptions = MutableMap<String, String>

enum class MMState {
  BOOTSTRAP, SHUTDOWN,
}

val debugMicroModule = Debugger("MicroModule")

abstract class MicroModule(val manifest: MicroModuleManifest) : IMicroModuleManifest by manifest {

  open val routers: Router? = null

  private var runningStateLock = StatePromiseOut.resolve(MMState.SHUTDOWN)

  private fun getModuleCoroutineScope() = CoroutineScope(SupervisorJob() + ioAsyncExceptionHandler)
  private var _scope: CoroutineScope = getModuleCoroutineScope()
  val ioAsyncScope get() = _scope

  val running get() = runningStateLock.value == MMState.BOOTSTRAP

  private suspend fun beforeBootstrap(bootstrapContext: BootstrapContext) {
    if (this.runningStateLock.state == MMState.BOOTSTRAP) {
      throw Exception("module ${this.mmid} already running");
    }
    this.runningStateLock.waitPromise() // 确保已经完成上一个状态
    this.runningStateLock = StatePromiseOut(MMState.BOOTSTRAP)
    this._bootstrapContext = bootstrapContext // 保存context
    // 创建一个新的
    if (!_scope.isActive) {
      _scope = getModuleCoroutineScope()
    }
  }

  private var _bootstrapContext: BootstrapContext? = null
  val bootstrapContext get() = _bootstrapContext ?: throw Exception("module no run.")

  protected abstract suspend fun _bootstrap(bootstrapContext: BootstrapContext)
  private suspend fun afterBootstrap(_dnsMM: BootstrapContext) {
    this.runningStateLock.resolve()
    debugMicroModule("afterBootstrap", "ready: $mmid")
    onConnect { (ipc) ->
      ipc.readyInMicroModule("onConnect")
    }
    for (ipc in _ipcSet) {
      ipc.readyInMicroModule("afterBootstrap")
    }
  }

  private fun Ipc.readyInMicroModule(tag: String) {
    debugMicroModule("ready/$tag", "(self)$mmid => ${remote.mmid}(remote)")
    ioAsyncScope.launch {
      this@readyInMicroModule.ready(this@MicroModule)
    }
  }

  suspend fun bootstrap(bootstrapContext: BootstrapContext) {
    this.beforeBootstrap(bootstrapContext)
    try {
      this._bootstrap(bootstrapContext);
    } finally {
      this.afterBootstrap(bootstrapContext);
    }
  }

  protected open suspend fun beforeShutdown() {
    if (this.runningStateLock.state == MMState.SHUTDOWN) {
      throw Exception("module $mmid already shutdown");
    }
    this.runningStateLock.waitPromise() // 确保已经完成上一个状态
    this.runningStateLock = StatePromiseOut(MMState.SHUTDOWN)

    /// 关闭所有的通讯
    _ipcSet.toList().forEach {
      it.close()
    }
    _ipcSet.clear()
  }

  protected abstract suspend fun _shutdown()
  protected open suspend fun afterShutdown() {
    _afterShutdownSignal.emitAndClear()
    _activitySignal.clear()
    _connectSignal.clear()
    runningStateLock.resolve()
    this._bootstrapContext = null
    // 取消所有的工作
    this.ioAsyncScope.cancel()
  }


  suspend fun shutdown() {
    this.beforeShutdown()
    try {
      this._shutdown()
    } finally {
      this.afterShutdown()
    }
  }

  protected val _afterShutdownSignal = SimpleSignal();
  val onAfterShutdown = _afterShutdownSignal.toListener()

  /**
   * 连接池
   */
  protected val _ipcSet = mutableSetOf<Ipc>();

  fun addToIpcSet(ipc: Ipc) {
    if (runningStateLock.isResolved && runningStateLock.value == MMState.BOOTSTRAP) {
      ipc.readyInMicroModule("addToIpcSet")
    }
    this._ipcSet.add(ipc)
    ipc.onClose {
      _ipcSet.remove(ipc)
    }
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
  protected fun onConnect(cb: Callback<IpcConnectArgs>) = _connectSignal.listen(cb);

  /**
   * 尝试连接到指定对象
   */
  suspend fun connect(mmid: MMID): Ipc {
    val (ipc) = this.bootstrapContext.dns.connect(mmid)
    return ipc
  }


  /**
   * 收到一个连接，触发相关事件
   */
  suspend fun beConnect(ipc: Ipc, reason: PureRequest) {
    this.addToIpcSet(ipc)
    ipc.onEvent { (event, ipc) ->
      if (event.name == "activity") {
        _activitySignal.emit(Pair(event, ipc))
      }
    }
    _connectSignal.emit(Pair(ipc, reason))
  }

  protected val _activitySignal = Signal<IpcActivityArgs>()
  protected fun onActivity(cb: Callback<IpcActivityArgs>) = _activitySignal.listen(cb)

//  /** 激活NMM入口*/
//  protected fun emitActivity(args)

  override fun toString(): String {
    return "MicroModule($mmid)"
  }

  open fun toManifest(): CommonAppManifest {
    return manifest.toCommonAppManifest()
  }
}

typealias IpcConnectArgs = Pair<Ipc, PureRequest>
typealias IpcActivityArgs = Pair<IpcEvent, Ipc>

//fun Uri.queryParameterByMap(): NativeOptions {
//    val hashMap = hashMapOf<String, String>()
//    this.queryParameterNames.forEach { name ->
//        val key = URLDecoder.decode(name, "UTF-8")
//        val value = URLDecoder.decode(this.getQueryParameter(name), "UTF-8")
//        hashMap[key] = value
//    }
//    return hashMap
//}

class StatePromiseOut<T>(val state: T) : PromiseOut<T>() {
  companion object {
    fun <T> resolve(state: T) = StatePromiseOut(state).also { it.resolve() }
  }

  fun resolve() {
    super.resolve(state)
  }
}