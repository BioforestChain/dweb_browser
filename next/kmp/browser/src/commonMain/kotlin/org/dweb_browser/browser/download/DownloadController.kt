package org.dweb_browser.browser.download

import io.ktor.http.ContentRange
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.close
import io.ktor.utils.io.core.ByteReadPacket
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import okio.IOException
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.constant.WindowMode
import org.dweb_browser.sys.window.core.helper.setFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.core.windowInstancesManager

@Serializable
data class DownloadTask(
  /** 下载编号 */
  val id: String,
  /** 下载链接 */
  val url: String,
  /** 创建时间 */
  val createTime: Long,
  /** 来源模块 */
  val originMmid: MMID,
  /** 来源链接 */
  val originUrl: String?,
  /** 下载回调链接 */
  val completeCallbackUrl: String?,
  /** 文件的元数据类型，可以用来做“打开文件”时的参考类型 */
  var mime: String,
  /** 文件路径 */
  var filepath: String,
  /** 标记当前下载状态 */
  val status: DownloadStateEvent = DownloadStateEvent()
) {

  @Transient
  var readChannel: ByteReadChannel? = null

  // 监听下载进度 不存储到内存
  @Transient
  val downloadSignal: Signal<DownloadTask> = Signal()

  @Transient
  val onDownload = downloadSignal.toListener()

  // 帮助实现下载暂停
  @Transient
  var paused = PromiseOut<Unit>()

  @Transient
  var pauseFlag = false
  suspend fun pauseWait() {
    if (pauseFlag) {
      debugDownload("DownloadTask", "下载暂停🚉${this.id}  ${this.status.current}")
      // 触发状态更新
      this.downloadSignal.emit(this)
      paused.waitPromise()
      // 还原状态
      this.status.state = DownloadState.Downloading
      paused = PromiseOut()
      pauseFlag = false
      debugDownload("DownloadTask", "下载恢复🍅")
    }
  }
}

@Serializable
enum class DownloadState {
  /** 初始化中，做下载前的准备，包括寻址、创建文件、保存任务等工作 */
  Init,

  /** 下载中*/
  Downloading,

  /** 暂停下载*/
  Paused,

  /** 取消下载*/
  Canceled,

  /** 下载失败*/
  Failed,

  /** 下载完成*/
  Completed,
}

@Serializable
data class DownloadStateEvent(
  var current: Long = 0,
  var total: Long = 1,
  var state: DownloadState = DownloadState.Init,
  var stateMessage: String = ""
)

class DownloadController(private val downloadNMM: DownloadNMM) {
  private val store = DownloadStore(downloadNMM)
  val downloadManagers: ChangeableMap<TaskId, DownloadTask> = ChangeableMap() // 用于监听下载列表
  val downloadCompletes: ChangeableMap<TaskId, DownloadTask> = ChangeableMap() // 用于下载完成或者下载失败
  private var winLock = Mutex(false)

  init {
    // 从内存中恢复状态
    downloadNMM.ioAsyncScope.launch {
      downloadManagers.putAll(store.getAll())
      // 状态改变的时候存储保存到内存
      downloadManagers.onChange {
        debugDownload(
          "DownloadController",
          "downloading add=${it.adds.size}, del=${it.removes.size}, upd=${it.updates.size}"
        )
        it.adds.forEach { key -> store.set(key, it.origin[key]!!) }
        it.removes.forEach { key -> store.delete(key) }
        it.updates.forEach { key -> store.set(key, it.origin[key]!!) }
      }
      downloadCompletes.putAll(store.getAllCompletes())
      downloadCompletes.onChange {
        debugDownload(
          "DownloadController",
          "complete add=${it.adds.size}, del=${it.removes.size}, upd=${it.updates.size}"
        )
        it.adds.forEach { key -> store.setComplete(key, it.origin[key]!!) }
        it.removes.forEach { key -> store.deleteComplete(key) }
        it.updates.forEach { key -> store.setComplete(key, it.origin[key]!!) }
      }
    }
  }

  /**
   * 执行下载任务 ,可能是断点下载
   */
  suspend fun downloadFactory(task: DownloadTask): Boolean {
    if (downloadNMM.exist(task.filepath)) {
      // 已经存在了，并且对方支持range 从断点开始
      val current = downloadNMM.info(task.filepath).size
      debugDownload("downloadFactory", "是否支持range:$current")

      // 已经存在并且下载完成
      if (current != null) {
        // 开始断点续传，这是在内存中恢复的，创建了一个新的channel
        downloadNMM.recover(task, ContentRange.TailFrom(current), this)
        task.status.current = current
        // 恢复状态 改状态为暂停，并且卡住
        task.status.state = DownloadState.Paused
        task.pauseFlag = true
        task.pauseWait()
      }
    }
    // 如果内存中没有，或者对方不支持Range，需要重新下载,否则这个channel是从支持的断点开始
    val stream = task.readChannel ?: return false
    debugDownload("downloadFactory", task.id)
    val buffer = this.middleware(task, stream)
    downloadNMM.appendFile(task, buffer)
    return true
  }


  /**
   * 下载 task 中间件
   */
  private fun middleware(downloadTask: DownloadTask, input: ByteReadChannel): ByteReadChannel {
    val output = ByteChannel(true)
    downloadTask.status.state = DownloadState.Downloading
    val taskId = downloadTask.id
    // 重要记录点 存储到硬盘
    downloadManagers[taskId] = downloadTask
    downloadNMM.ioAsyncScope.launch {
      debugDownload("middleware", "id:$taskId current:${downloadTask.status.current}")
      downloadTask.downloadSignal.emit(downloadTask)
      try {
        input.consumeEachArrayRange { byteArray, last ->
          // 处理是否暂停
          downloadTask.pauseWait()
          if (output.isClosedForRead) {
            breakLoop()
            downloadTask.status.state = DownloadState.Canceled
            // 触发取消
            input.cancel()
            downloadTask.downloadSignal.emit(downloadTask)
            // 重要记录点 存储到硬盘
            // downloadManagers[taskId] = downloadTask
            downloadManagers.remove(taskId)?.let {
              downloadCompletes[taskId] = it
            }
          } else if (last) {
            output.close()
            input.cancel()
            downloadTask.status.state = DownloadState.Completed
            // 触发完成
            downloadTask.downloadSignal.emit(downloadTask)
            // 重要记录点 存储到硬盘
            // downloadManagers[taskId] = downloadTask
            downloadManagers.remove(taskId)?.let {
              downloadCompletes[taskId] = it
            }
          } else {
            downloadTask.status.current += byteArray.size
            // 触发进度更新
            downloadTask.downloadSignal.emit(downloadTask)
            output.writePacket(ByteReadPacket(byteArray))
          }
        }
      } catch (e: IOException) {
        // 这里捕获的一般是 connection reset by peer 当前没有重试机制，用户再次点击即为重新下载
        debugDownload("middleware", "${e.message}")
        downloadTask.status.state = DownloadState.Failed
        // 触发失败
        downloadTask.downloadSignal.emit(downloadTask)
        // 内存中删除
        downloadManagers.remove(taskId)?.let {
          downloadCompletes[taskId] = it
        }
      }
    }
    return output
  }

  /**
   * 窗口是单例模式
   */
  private var win: WindowController? = null
  suspend fun renderDownloadWindow(wid: UUID) = winLock.withLock {
    (windowInstancesManager.get(wid) ?: throw Exception("invalid wid: $wid")).also { newWin ->
      if (win == newWin) {
        return@withLock
      }
      win = newWin
      newWin.state.apply {
        mode = WindowMode.MAXIMIZE
        setFromManifest(downloadNMM)
      }
      /// 提供渲染适配
      windowAdapterManager.provideRender(wid) { modifier ->
        Render(modifier, this)
      }
      newWin.onClose {
        winLock.withLock {
          if (newWin == win) {
            win = null
          }
        }
      }
    }
  }
}