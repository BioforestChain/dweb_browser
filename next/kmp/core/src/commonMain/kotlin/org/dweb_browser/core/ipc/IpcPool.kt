package org.dweb_browser.core.ipc

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.helper.EndpointIpcMessage
import org.dweb_browser.core.ipc.helper.IpcPoolMessageArgs
import org.dweb_browser.core.ipc.helper.OnIpcPoolMessage
import org.dweb_browser.core.ipc.helper.normalizeIpcMessage
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SuspendOnce1
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.http.PureStream

val debugIpcPool = Debugger("ipc")

val kotlinIpcPool = IpcPool()

data class IpcOptions(
  /**远程的模块是谁*/
  val remote: IMicroModuleManifest,
  /**是否自动发送握手消息*/
//  val autoStart: Boolean = true,
  /**当endpoint为Worker的时候需要传递*/
  val port: WebMessageEndpoint? = null,
  /**当endpoint为Kotlin的时候需要传递*/
  val channel: NativeEndpoint? = null,
  /**当endpoint为FrontEnd的时候需要传递*/
//  val stream: PureStream? = null
)

/**
 * IpcPool跟上下文对应，跟body流对应
 * Context(Kotlin,Worker,Front)
 * */
open class IpcPool {
  companion object {
    private fun randomPoolId() = "kotlin-${randomUUID()}"
  }

  val scope =
    CoroutineScope(CoroutineName("ipc-pool-kotlin") + ioAsyncExceptionHandler + SupervisorJob())

  /**每一个ipcPool都会绑定一个body流池,只有当不在同一个IpcPool的时候才需要互相拉取*/
  val poolId: UUID = randomPoolId()

  override fun toString() = "IpcPool@poolId=$poolId<uid=$poolId>"

  /**
   * 所有的ipc对象实例集合
   */
  private val ipcSet = mutableSetOf<Ipc>()

  /**
   * 所有的委托进来的流的实例集合
   */
  private val streamPool = mutableMapOf<String, PureStream>()
  fun createIpc(
    endpoint: IpcEndpoint,
    locale: IMicroModuleManifest,
    remote: IMicroModuleManifest,
    autoStart: Boolean = true,
  ) = Ipc(endpoint = endpoint, locale = locale, remote = remote, pool = this).also { ipc ->
    /// 保存ipc，并且根据它的生命周期做自动删除
    debugIpcPool("createIpc") { "add ${ipc.debugId}" }
    ipcSet.add(ipc)
    scope.launch {
      /// 自动启动
      if (autoStart) {
        ipc.start()
      }

      debugIpcPool("createIpc") { "remove ${ipc.debugId}" }
      ipcSet.remove(ipc)
    }
  }


  private var accPid = atomic(0)

  /**
   * 根据传进来的业务描述，注册一个Pid
   */
  internal fun generatePid() = accPid.updateAndGet { it + 1 }

  // 收消息
  private val _messageSignal = Signal<IpcPoolMessageArgs>()
  suspend fun dispatchMessage(args: IpcPoolMessageArgs) = _messageSignal.emit(args)
  suspend fun dispatchMessage(pack: EndpointIpcMessage, ipc: Ipc) =
    _messageSignal.emit(IpcPoolMessageArgs(normalizeIpcMessage(pack, ipc), ipc))

  fun onMessage(cb: OnIpcPoolMessage) = _messageSignal.listen(cb)

  /**
   * 分发到各个Ipc(endpoint)
   * 为了防止消息丢失，得有一开始的握手和消息断开
   */
  init {
    onMessage { (pack, ipc) ->
      ipc.dispatchMessage(pack.ipcMessage)
    }
  }


  /**关闭信号*/
  suspend fun awaitDestroyed() = runCatching {
    scope.coroutineContext[Job]!!.join();
    null
  }.getOrElse { it }

  val isDestroyed get() = scope.coroutineContext[Job]!!.isCancelled

  val destroy = SuspendOnce1 { cause: CancellationException ->
    val oldSet = ipcSet.toSet()
    ipcSet.clear()
    oldSet.forEach { ipc ->
      ipc.close()
    }
    scope.cancel(cause)
  }

}