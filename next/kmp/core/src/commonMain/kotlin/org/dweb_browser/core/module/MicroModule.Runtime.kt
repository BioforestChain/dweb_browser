package org.dweb_browser.core.module

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import org.dweb_browser.core.help.types.CommonAppManifest
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.help.types.MicroModuleManifest
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.std.permission.PermissionProvider
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.SafeHashSet
import org.dweb_browser.helper.defaultAsyncExceptionHandler
import org.dweb_browser.helper.listen
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
    abstract val bootstrapContext: BootstrapContext
    val scope =
      CoroutineScope(SupervisorJob() + defaultAsyncExceptionHandler + CoroutineName(manifest.mmid))

    open val routers: Router? = null

    private fun getModuleCoroutineScope() =
      CoroutineScope(SupervisorJob() + defaultAsyncExceptionHandler + CoroutineName(mmid))

    val mmScope = getModuleCoroutineScope() // 给外部使用

    private val stateAtomic = atomic(MMState.SHUTDOWN)
    val isRunning get() = stateAtomic.value == MMState.BOOTSTRAP


    protected abstract suspend fun _bootstrap()

    private val beforeBootstrapFlow = MutableSharedFlow<Unit>()
    val onBeforeBootstrap = beforeBootstrapFlow.shareIn(mmScope, SharingStarted.Eagerly)


    private val afterBootstrapFlow = MutableSharedFlow<Unit>()
    val onAfterBootstrap = afterBootstrapFlow.shareIn(mmScope, SharingStarted.Eagerly)

    suspend fun bootstrap() = stateAtomic.update {
      if (it != MMState.BOOTSTRAP) {
        beforeBootstrapFlow.emit(Unit)
        _bootstrap();
        afterBootstrapFlow.emit(Unit)
      } else {
        debugMicroModule("beforeBootstrap", "$mmid already running")
      }
      MMState.BOOTSTRAP
    }


    private val beforeShutdownFlow = MutableSharedFlow<Unit>()
    val onBeforeShutdown = beforeShutdownFlow.shareIn(mmScope, SharingStarted.Eagerly)

    private val afterShutdownFlow = MutableSharedFlow<Unit>()
    val onAfterShutdown = afterShutdownFlow.shareIn(mmScope, SharingStarted.Eagerly)

//
//    /**
//     * 让回调函数一定在启动状态内被运行
//     */
//    suspend fun <R> withBootstrap(block: suspend () -> R) = readyLock.withLock { block() }

    protected abstract suspend fun _shutdown()

    suspend fun shutdown() = stateAtomic.update {
      if (it != MMState.SHUTDOWN) {
        beforeShutdownFlow.emit(Unit)
        _shutdown()
        afterShutdownFlow.emit(Unit)
        // 取消所有的工作
        this.mmScope.cancel()
      }
      MMState.SHUTDOWN
    }

    /**
     * MicroModule 引用池
     */
    private val connectionLinks = SafeHashSet<Ipc>()

    /**
     *
     */
    fun linkIpc(ipc: Ipc): Boolean {
      return if (this.connectionLinks.add(ipc)) {
        onBeforeShutdown.listen {
          ipc.close()
        }
        ipc.onBeforeClose.listen {
          connectionLinks.remove(ipc)
        }
        true
      } else false
    }

    /**
     * 内部程序与外部程序通讯的方法
     */
    private val connectFlow = MutableSharedFlow<IpcConnectArgs>();

    /**
     * 给内部程序自己使用的 onConnect，外部与内部建立连接时使用
     * 因为 NativeMicroModule 的内部程序在这里编写代码，所以这里会提供 onConnect 方法
     * 如果时 JsMicroModule 这个 onConnect 就是写在 WebWorker 那边了
     */
    val onConnect = connectFlow.shareIn(mmScope, SharingStarted.Lazily)

    private val connectionMap = SafeHashMap<MMID, Deferred<Ipc>>()

    /**
     * 尝试连接到指定对象
     */
    suspend fun connect(mmid: MMID, reason: PureRequest? = null) = connectionMap.getOrPut(mmid) {
      mmScope.async {
        bootstrapContext.dns.connect(mmid, reason).also {
          linkIpc(it)
        }
      }
    }.await()

    /**
     * 收到一个连接，触发相关事件
     */
    suspend fun beConnect(ipc: Ipc, reason: PureRequest?) {
      if (this.linkIpc(ipc)) {
        // 尝试保存到连接池中
        @Suppress("DeferredResultUnused") connectionMap.getOrPut(ipc.remote.mmid) {
          CompletableDeferred(
            ipc
          )
        }
        connectFlow.emit(IpcConnectArgs(ipc, reason))
      }
    }

  }

  private val runtimeAtomic = atomic<Runtime?>(null)
  val isRunning get() = runtimeAtomic.value != null
  val runtimeOrNull get() = runtimeAtomic.value
  val runtime get() = runtimeAtomic.value ?: throw IllegalStateException("$this is no running")
  fun bootstrap(bootstrapContext: BootstrapContext) {
    runtimeAtomic.updateAndGet {
      it ?: createRuntime(bootstrapContext)
    }
  }

  abstract fun createRuntime(bootstrapContext: BootstrapContext): Runtime

  override fun toString(): String {
    return "MicroModule($mmid)"
  }

  open fun toManifest(): CommonAppManifest {
    return manifest.toCommonAppManifest()
  }
}

data class IpcConnectArgs(val ipc: Ipc, val reason: PureRequest?)
