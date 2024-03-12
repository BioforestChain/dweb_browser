package org.dweb_browser.core.ipc

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.helper.IpcMessageArgs
import org.dweb_browser.core.ipc.helper.IpcPoolMessageArgs
import org.dweb_browser.core.ipc.helper.IpcPoolPack
import org.dweb_browser.core.ipc.helper.OnIpcPoolMessage
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.http.PureStream

val debugIpcPool = Debugger("ipc")

val kotlinIpcPool = IpcPool()

//enum class Endpoint {
//  Kotlin, Worker, FrontEnd
//}

data class IpcOptions(
  /**远程的模块是谁*/
  val remote: IMicroModuleManifest,
  /**是否自动发送握手消息*/
//  val autoStart: Boolean = true,
  /**当endpoint为Worker的时候需要传递*/
  val port: MessagePort? = null,
  /**当endpoint为Kotlin的时候需要传递*/
  val channel: NativePort<IpcPoolPack, IpcPoolPack>? = null,
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
    private val ipcPoolScope =
      CoroutineScope(CoroutineName("ipc-pool-kotlin") + ioAsyncExceptionHandler)
  }

  /**每一个ipcPool都会绑定一个body流池,只有当不在同一个IpcPool的时候才需要互相拉取*/
  val poolId: UUID = randomPoolId()

  /**body  流池 <streamId,PureStream> */
  val streamPool = mutableMapOf<String, PureStream>()
  override fun toString() = "IpcPool@poolId=$poolId<uid=$poolId>"

  // 用来路由和自动切换
  private val ipcPool = mutableMapOf<String, Ipc>()

//  // 一个消耗通信的机制，确保消息
//  internal val consumptionLife = mutableMapOf<String, Int>()
//
//  // 重试保险丝次数
//  internal val reFuse = 3

  /**
   * fork出一个已经创建好通信的ipc, 这里会等待
   */
  suspend fun <T : Ipc> create(
    channelId: String, options: IpcOptions
  ): T {
    return ipcPool.getOrPut(channelId) {
      val mm = options.remote
      // 创建不同的Ipc
      val ipc = if (options.port != null) {
        MessagePortIpc(options.port, mm, channelId, this)
      } else if (options.channel != null) {
        NativeIpc(options.channel, mm, channelId, this)
      } else {
        ReadableStreamIpc(channelId, mm, this)
      }
      ipc.start()
      return ipc as T
    } as T
  }

  /**
   * 根据传进来的业务描述，注册一个Pid
   */
  internal fun generatePid(channelId: String): Int {
    val time = datetimeNow()
    return "${channelId}${time}".hashCode()
  }

  // 收消息
  private val _messageSignal = Signal<IpcPoolMessageArgs>()
  suspend fun emitMessage(args: IpcPoolMessageArgs) = _messageSignal.emit(args)
  fun onMessage(cb: OnIpcPoolMessage) = _messageSignal.listen(cb)

  /**
   * 分发到各个Ipc(endpoint)
   * 为了防止消息丢失，得有一开始的握手和消息断开
   */
  init {
    onMessage { (pack, ipc) ->
      ipc.emitMessage(IpcMessageArgs(pack.ipcMessage, ipc))
    }
  }

  /**-----close start*/

  //关闭信号
  val closeSignal = SimpleSignal()

  /**用来释放一些跟ipcPool绑定的资源*/
  val onClose = this.closeSignal.toListener()
  private var _closed = false
  val isClosed get() = _closed
  suspend fun close() {
    if (this._closed) {
      return
    }
    this._closed = true
    this.closeSignal.emitAndClear()
  }

  /**-----close end*/
}