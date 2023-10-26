package org.dweb_browser.browser.download

import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.close
import io.ktor.utils.io.core.ByteReadPacket
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.toUtf8

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
  val mime: String,
  /** 文件路径 */
  val filepath: String,
) {
  /** 标记当前下载状态 */
  var status: DownloadStateEvent = DownloadStateEvent()

  @Transient
  var readChannel: ByteReadChannel? = null

  // 监听下载进度 不存储到内存
  @Transient
  val downloadSignal: Signal<DownloadTask> = Signal()

  @Transient
  val onDownload = downloadSignal.toListener()
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

private typealias taskId = String

class DownloadController(val mm: DownloadNMM) {
  var downloadManagers: MutableMap<taskId, DownloadTask> = mutableMapOf() // 用于监听下载列表
  val store = DownloadStore(mm)

  // 状态改变监听
  private val downloadState: Signal<Pair<String, DownloadStateEvent>> = Signal()
  private val onState = downloadState.toListener()

  init {
    // 从内存中恢复状态
    mm.ioAsyncScope.launch {
      downloadManagers = store.getAll()
      // 状态改变的时候存储保存到内存
      onState { (id, event) ->
        downloadManagers[id]?.let { task ->
          task.status = event
          store.set(id, task)
        }
      }
    }
  }

  /**
   * 下载 task 中间件
   */
  fun middleware(downloadTask: DownloadTask, input: ByteReadChannel): ByteReadChannel {
    val output = ByteChannel(true)
    downloadTask.status.state = DownloadState.Downloading
    mm.ioAsyncScope.launch {
      downloadTask.downloadSignal.emit(downloadTask)
      input.consumeEachArrayRange { byteArray, last ->
        debugDownload("middleware","byteArray:${byteArray.toUtf8()}")
        if (output.isClosedForRead) {
          breakLoop()
          downloadTask.status.state = DownloadState.Canceled
          // 触发取消
          input.cancel()
          downloadTask.downloadSignal.emit(downloadTask)
        } else if (last) {
          output.close()
          input.cancel()
          downloadTask.status.state = DownloadState.Completed
          // 触发完成
          downloadTask.downloadSignal.emit(downloadTask)
        } else {
          downloadTask.status.current += byteArray.size
          // 触发进度更新
          downloadTask.downloadSignal.emit(downloadTask)
          output.writePacket(ByteReadPacket(byteArray))
        }
      }
    }
    return output
  }
}