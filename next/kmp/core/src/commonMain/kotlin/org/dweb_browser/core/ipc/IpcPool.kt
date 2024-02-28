package org.dweb_browser.core.ipc

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
      CoroutineScope(CoroutineName("ipc-message") + ioAsyncExceptionHandler)
    private val poolMutex = Mutex()

    //缓存消息，需要过一段时间进行回收
    private val cacheIpcPackList = mutableListOf<IpcPoolPack>()
  }

  // body 计数,每一个ipcPool都会绑定一个body流
  val poolId: UUID = randomPoolId()

  // 用于存储Ipc协商的
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
    val pid = generatePid(channelId)
    var ipc: Ipc? = null
    if (options.port != null) {
      ipc = MessagePortIpc(options.port, options.remote, channelId, this)
    } else if (options.channel != null) {
      ipc = NativeIpc(options.channel, options.remote, channelId, this)
    } else if (options.stream != null) {
      ipc = ReadableStreamIpc(
        options.remote,
        channelId,
        this
      ).bindIncomeStream(options.stream)
    }
    if (ipc == null) {
      throw Exception("create ipc error")
    }
    ipcHashMap[pid] = ipc
    ipc.lifeCycleHook()
    if (!ipc.startDeferred.isCompleted) {
      ipc.start()
    }
    ipc as T
  }

  /**生命周期初始化，协商数据格式*/
  private fun Ipc.lifeCycleHook() {
    // TODO 跟对方通信 协商数据格式
    this.onLifeCycle { (lifeCycle, ipc) ->
      debugIpc("lifeCycleHook=>", lifeCycle.state)
      when (lifeCycle.state) {
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
    // 只要注册过了，只有注册的那个人可以使用
    if (pid != null) {
      throw Exception("The ipc already exists!")
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
    ipcPoolScope.launch {
      // 监听ipc建立连接成功
      ipcHashMap.onChange { (ipcMap, add) ->
        // 消耗没有发送的消息
        cacheIpcPackList.filter { pack ->
          if (pack.pid == add.first()) {
            val ipc = ipcMap[pack.pid]
            debugIpcPool("处理消息：${add.first()} ${pack.pid} ${ipc?.remote?.mmid}")
            if (ipc == null) return@filter true // 消息未被消耗
            ipc.postMessage(pack.ipcMessage)
            return@filter false  // 消息已经消耗了
          }
          return@filter true // 消息未被消耗
        }
      }
    }
    onMessage { (pack) ->
      val ipc = ipcHashMap[pack.pid]
      if (ipc == null) {
        cacheIpcPackList.add(pack)
      } else {
        ipc.emitMessage(IpcMessageArgs(pack.ipcMessage, ipc))
      }
    }
  }

  /**-----close start*/
  private var _closed = false
  val isClosed get() = _closed
  suspend fun close() {
    if (this._closed) {
      return
    }
    this._closed = true
    this.closeSignal.emitAndClear()
    this.ipcChannelMap.clear()

    /// 关闭的时候会自动触发销毁
    this.destroy(false)
  }

  //关闭信号
  val closeSignal = SimpleSignal()
  val onClose = this.closeSignal.toListener()

  //销毁信号
  private val _destroySignal = SimpleSignal()
  val onDestroy = this._destroySignal.toListener()

  private var _destroyed = false
  val isDestroy get() = _destroyed

  /**
   * 销毁实例
   */
  suspend fun destroy(close: Boolean = true) {
    if (_destroyed) {
      return
    }
    _destroyed = true
    if (close) {
      this.close()
    }
    this._destroySignal.emitAndClear()
  }

  /**-----close end*/
}