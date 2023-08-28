package org.dweb_browser.microservice.core

import org.dweb_browser.helper.Callback
import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.ShortcutItem
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.microservice.help.CommonAppManifest
import org.dweb_browser.microservice.help.DWEB_DEEPLINK
import org.dweb_browser.microservice.help.IpcSupportProtocols
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.MMID
import org.dweb_browser.microservice.help.MicroModuleManifest
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.http4k.core.Request

typealias Router = MutableMap<String, AppRun>
typealias AppRun = (options: NativeOptions) -> Any
typealias NativeOptions = MutableMap<String, String>

enum class MMState {
  BOOTSTRAP, SHUTDOWN,
}

abstract class MicroModule : MicroModuleManifest() {
  abstract override val mmid: MMID
  abstract override val dweb_deeplinks: List<DWEB_DEEPLINK>
  abstract override val categories: MutableList<MICRO_MODULE_CATEGORY>
  abstract override val dir: String?
  abstract override val lang: String?
  abstract override val name: String
  abstract override val short_name: String
  abstract override val description: String?
  abstract override val icons: List<ImageResource>
  abstract override val display: DisplayMode?
  abstract override val orientation: String?
  abstract override val screenshots: List<ImageResource>?
  abstract override val shortcuts: List<ShortcutItem>
  abstract override val theme_color: String?
  abstract override val ipc_support_protocols: IpcSupportProtocols
  abstract override val background_color: String?

  open val routers: Router? = null

  private var runningStateLock = StatePromiseOut.resolve(MMState.SHUTDOWN)
  val running get() = runningStateLock.value == MMState.BOOTSTRAP

  private suspend fun beforeBootstrap(bootstrapContext: BootstrapContext) {
    if (this.runningStateLock.state == MMState.BOOTSTRAP) {
      throw Exception("module ${this.mmid} already running");
    }
    this.runningStateLock.waitPromise() // 确保已经完成上一个状态
    this.runningStateLock = StatePromiseOut(MMState.BOOTSTRAP)
    this._bootstrapContext = bootstrapContext // 保存context
  }

  private var _bootstrapContext: BootstrapContext? = null
  val bootstrapContext get() = _bootstrapContext ?: throw Exception("module no run.")

  protected abstract suspend fun _bootstrap(bootstrapContext: BootstrapContext)
  private suspend fun afterBootstrap(_dnsMM: BootstrapContext) {
    this.runningStateLock.resolve()
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
  suspend fun beConnect(ipc: Ipc, reason: Request) {
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
    return MicroModuleManifest(
      mmid = mmid,
      dweb_deeplinks = dweb_deeplinks,
      ipc_support_protocols = ipc_support_protocols,
      categories = categories,
      dir = dir,
      lang = lang,
      name = name,
      short_name = short_name,
      description = description,
      icons = icons,
      display = display,
      orientation = orientation,
      screenshots = screenshots,
      shortcuts = shortcuts,
      theme_color = theme_color,
      background_color = background_color,
    )
  }
}

typealias IpcConnectArgs = Pair<Ipc, Request>
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
