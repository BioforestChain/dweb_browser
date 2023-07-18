package org.dweb_browser.microservice.core

import org.dweb_browser.helper.*
import org.dweb_browser.microservice.help.DWEB_DEEPLINK
import org.dweb_browser.microservice.help.Mmid
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.http4k.core.*

typealias Router = MutableMap<String, AppRun>
typealias AppRun = (options: NativeOptions) -> Any
typealias NativeOptions = MutableMap<String, String>

enum class MMState {
  BOOTSTRAP,
  SHUTDOWN,
}

abstract class MicroModule : Ipc.MicroModuleInfo {
  override val mmid: Mmid = ""
  override val dweb_deeplinks = mutableListOf<DWEB_DEEPLINK>()
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
  private suspend fun afterBootstrap(dnsMM: BootstrapContext) {
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

  protected suspend fun beforeShutdown() {
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

  fun onAfterShutdown(cb: Callback<Unit>) = _afterShutdownSignal.listen(cb)
  protected val _afterShutdownSignal = SimpleSignal();

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
  protected fun onConnect(cb: Callback<IpcConnectArgs>) =
    _connectSignal.listen(cb);

  /**
   * 尝试连接到指定对象
   */
  suspend fun connect(mmid: Mmid, reason: Request? = null) =
    this.bootstrapContext.dns.let {
      it.open(mmid)
      it.connect(mmid)
    }


  /**
   * 收到一个连接，触发相关事件
   */
  suspend fun beConnect(ipc: Ipc, reason: Request) {
    this.addToIpcSet(ipc)
    ipc.onEvent { (event, ipc) ->
      if (event.name == "activity") {
        onActivity(event, ipc)
      }
    }
    _connectSignal.emit(Pair(ipc, reason))
  }


  /** 激活NMM入口*/
  protected open suspend fun onActivity(event: IpcEvent, ipc: Ipc) {}

  override fun toString(): String {
    return "MicroModule($mmid)"
  }

//    protected var apiRouting: RoutingHttpHandler? = null
//    protected val requestContexts = RequestContexts()
//    protected val requestContextKey_ipc = RequestContextKey.required<Ipc>(requestContexts)
//
//    protected fun defineHandler(handler: suspend (request: Request) -> Any?) = { request: Request ->
//        runBlockingCatching {
//            when (val result = handler(request)) {
//                null, Unit -> {
//                    Response(Status.OK)
//                }
//                is Response -> result
//                is ByteArray -> Response(Status.OK).body(MemoryBody(result))
//                is InputStream -> Response(Status.OK).body(result)
//                else -> {
//                    // 如果有注册处理函数，那么交给处理函数进行处理
//                    NativeMicroModule.ResponseRegistry.handle(result)
//                }
//            }
//        }.getOrElse { ex ->
//            debugDNS("NMM/Error", request.uri, ex)
//            Response(Status.INTERNAL_SERVER_ERROR).body(
//                """
//                    <p>${request.uri}</p>
//                    <pre>${ex.message ?: "Unknown Error"}</pre>
//                    """.trimIndent()
//
//            )
//        }
//
//    }
//
//    protected fun defineHandler(handler: suspend (request: Request, ipc: Ipc) -> Any?) =
//        defineHandler { request ->
//            handler(request, requestContextKey_ipc(request))
//        }
//
}

typealias IpcConnectArgs = Pair<Ipc, Request>

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
    fun <T> resolve(state: T) =
      StatePromiseOut(state).also { it.resolve() }
  }

  fun resolve() {
    super.resolve(state)
  }
}
