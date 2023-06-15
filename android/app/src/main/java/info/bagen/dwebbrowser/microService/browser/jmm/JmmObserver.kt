package info.bagen.dwebbrowser.microService.browser.jmm

import org.dweb_browser.microservice.help.Mmid
import org.dweb_browser.helper.*
import info.bagen.dwebbrowser.microService.browser.jmm.ui.DownLoadStatus
import info.bagen.dwebbrowser.util.moreThanTwoDigits
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class DownLoadObserverListener(
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
        val listener = DownLoadObserverListener(status, downLoadSize, totalSize)
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
    state = MutableStateFlow(DownLoadObserverListener(DownLoadStatus.IDLE))
    flow = state.asSharedFlow()
  }

  suspend fun observe(cb: FlowCollector<DownLoadObserverListener>) {
    flow.collect(cb)
  }

  fun close() {
    downloadMap[this.mmid]?.remove(this)
  }
}