package org.dweb_browser.browser.download.model

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import okio.Path.Companion.toPath
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.datetimeNow

@Serializable
data class DownloadTask(
  /** 下载编号 */
  val id: String,
  /** 下载链接 */
  val url: String,
  /** 创建时间 */
  val createTime: Long = datetimeNow(),
  /** 来源模块 */
  val originMmid: MMID,
  /** 来源链接 */
  val originUrl: String?,
  /** 打开应用的跳转地址 */
  val openDappUri: String?,
  /** 文件的元数据类型，可以用来做“打开文件”时的参考类型 */
  var mime: String,
  /** 文件路径 */
  var filepath: String,
  /** 标记当前下载状态 */
  val status: DownloadStateEvent,
) {
  val filename by lazy {
    filepath.toPath().name
  }

  @Transient
  var readChannel: ByteReadChannel? = null

  // 监听下载进度 不存储到数据库
  @Transient
  private val changeFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

  fun emitChanged() {
    changeFlow.tryEmit(Unit)
  }

  @Transient
  val onChange = changeFlow.asSharedFlow()

  @Transient
  internal val paused = Mutex()

  fun cancel() {
    status.state = DownloadState.Canceled
    status.current = 0L
    readChannel?.cancel()
    readChannel = null
  }

  @Transient
  var external: Boolean = false
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
  var stateMessage: String = "",
) {
  fun progress(): Float {
    return if (total == 0L) {
      0f
    } else {
      (current * 1.0f / total) * 10 / 10.0f
    }
  }

  fun percentProgress(): String {
    return if (total == 0L) {
      "0 %"
    } else {
      "${(current * 1000 / total) / 10.0f} %"
    }
  }
}