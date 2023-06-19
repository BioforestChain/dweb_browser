package org.dweb_browser.browserUI.download

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.dweb_browser.dwebview.serviceWorker.DownloadControllerEvent
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.microservice.help.Mmid
import java.math.RoundingMode
import java.text.DecimalFormat

enum class DownLoadStatus {
  IDLE, DownLoading, DownLoadComplete, PAUSE, INSTALLED, FAIL, CANCEL, NewVersion;

  fun toServiceWorkerEvent() =
    when (this) {
      IDLE -> DownloadControllerEvent.Start.event
      PAUSE -> DownloadControllerEvent.Pause.event
      CANCEL -> DownloadControllerEvent.Cancel.event
      DownLoading -> DownloadControllerEvent.Progress.event
      else -> DownloadControllerEvent.End.event
    }
}

data class DownLoadInfo(
  val id: Mmid,
  val url: String, // 下载地址
  val name: String, // 软件名称
  var path: String = "", // 文件下载路径
  var notificationId: Int = 0, // 通知栏的id
  var size: Long = 0L, // 文件大小
  var dSize: Long = 1L, // 已下载大小
  // var progress: Float = 0f, // 进度 0~1
  var downLoadStatus: DownLoadStatus = DownLoadStatus.IDLE, // 标记当前下载状态
  val appInfo:String = "", // 保存app数据，如jmmMetadata
)

data class DownLoadObserverListener(
  val mmid: Mmid,
  val downLoadStatus: DownLoadStatus,
  val downLoadSize: Long = 0L,
  val totalSize: Long = 1L,
  val progress: String = (1.0f * downLoadSize / totalSize).moreThanTwoDigits()
)

class DownLoadObserver(private val mmid: Mmid) {
  companion object {
    private val downloadMap = mutableMapOf<Mmid, MutableList<DownLoadObserver>>()

    fun emit(
      mmid: Mmid, status: DownLoadStatus, downLoadSize: Long = 0L, totalSize: Long = 1L
    ) {
      runBlockingCatching(ioAsyncExceptionHandler) {
        val listener = DownLoadObserverListener(mmid, status, downLoadSize, totalSize)
        downloadMap[mmid]?.forEach { observer -> observer.state.emit(listener) }
      }
    }

    fun close(mmid: Mmid) {
      downloadMap.remove(mmid)
    }
  }

  private var state: MutableStateFlow<DownLoadObserverListener>
  private var flow: SharedFlow<DownLoadObserverListener>

  init {
    downloadMap.getOrPut(mmid) { mutableListOf() }.add(this)
    state = MutableStateFlow(DownLoadObserverListener(mmid, DownLoadStatus.IDLE))
    flow = state.asSharedFlow()
  }

  suspend fun observe(cb: FlowCollector<DownLoadObserverListener>) {
    flow.collect(cb)
  }

  fun close() {
    downloadMap[this.mmid]?.remove(this)
  }
}

val Float.moreThanTwoDigits: () -> String
  get() = {
    val format = DecimalFormat("#.##")
    //舍弃规则，RoundingMode.FLOOR表示直接舍弃。
    format.roundingMode = RoundingMode.FLOOR
    format.format(this)
  }