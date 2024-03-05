package org.dweb_browser.core.ipc

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.helper.IPC_STATE
import org.dweb_browser.core.ipc.helper.IpcLifeCycle
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.core.ipc.helper.IpcMessageArgs
import org.dweb_browser.core.ipc.helper.IpcPoolMessageArgs
import org.dweb_browser.core.ipc.helper.IpcPoolPack
import org.dweb_browser.core.ipc.helper.OnIpcPoolMessage
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.UUID
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
  val stream: PureStream? = null
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
    private val poolMutex = Mutex()
  }

  /**每一个ipcPool都会绑定一个body流池,只有当不在同一个IpcPool的时候才需要互相拉取*/
  val poolId: UUID = randomPoolId()

  /**body  流池 <streamId,PureStream> */
  val streamPool = mutableMapOf<String, PureStream>()

  // 用于存储channelId 对应的 pid
  private val ipcChannelMap = HashMap<String, Int>()
  private val ipcHashMap: ChangeableMap<Int, Ipc> = ChangeableMap()

  override fun toString() = "IpcPool@poolId=$poolId<uid=$poolId>"

  /**
   * fork出一个已经创建好通信的ipc
   * @options IpcOptions
   */
  suspend fun <T : Ipc> create(
    /**ipc的业务线标识*/
    channelId: String,
    options: IpcOptions
  ) = poolMutex.withLock {
    val mm = options.remote
    // 创建不同的Ipc
    val ipc = if (options.port != null) {
      MessagePortIpc(options.port, mm, channelId, this)
    } else if (options.channel != null) {
      NativeIpc(options.channel, mm, channelId, this)
    } else {
      ReadableStreamIpc(
        channelId,
        mm,
        this
      )
    }
    if (ipc is ReadableStreamIpc && options.stream != null) {
      ipc.bindIncomeStream(options.stream)
    }
    //  有新的ipc激活了
    val pid = generatePid(channelId)
    ipcHashMap[pid] = ipc
    ipc.lifeCycleHook()
    // 如果还没启动，自我启动一下
    if (!ipc.startDeferred.isCompleted) {
      if (!(ipc is ReadableStreamIpc && !ipc.isBinding)) {
        ipc.start()
      }
    }
    ipc as T
  }

  /**生命周期初始化，协商数据格式*/
  private fun Ipc.lifeCycleHook() {
    // TODO 跟对方通信 协商数据格式
    this.onLifeCycle { (lifeCycle, ipc) ->
      debugIpc("lifeCycleHook=>", lifeCycle.state)
      when (lifeCycle.state) {
        // 收到打开中的消息，也告知自己已经准备好了
        IPC_STATE.OPENING -> {
          ipc.postMessage(IpcLifeCycle(IPC_STATE.OPEN))
        }
        // 收到对方完成开始建立连接
        IPC_STATE.OPEN -> {
          if (!ipc.startDeferred.isCompleted) {
            ipc.startDeferred.complete(lifeCycle)
          }
        }
        // 消息通道开始关闭
        IPC_STATE.CLOSING -> {
          ipc.closing()
        }
        // 对方关了，代表没有消息发过来了，我也关闭
        IPC_STATE.CLOSED -> {
          ipc.close()
        }
      }
    }
  }

  /**
   * 根据传进来的业务描述，注册一个Pid
   */
  private fun generatePid(channelId: String): Int {
    val pid = ipcChannelMap[channelId]
    if (pid != null) {
      return pid
    }
    val hashPid = "${channelId}${pid}".hashCode()
    ipcChannelMap[channelId] = hashPid
    return hashPid
  }

  // 发消息
  suspend fun doPostMessage(channelId: String, data: IpcMessage) {
    // TODO waterbang 这里不想每次获取两个map
    val pid = ipcChannelMap[channelId] ?: throw Exception("this channelId $poolId not found!")
    val ipc = ipcHashMap[pid] ?: throw Exception("this ipc $pid not found!")

    ipc.doPostMessage(pid, data)
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
    // 释放自己内部的资源
    this.ipcChannelMap.clear()
  }

  /**-----close end*/
}