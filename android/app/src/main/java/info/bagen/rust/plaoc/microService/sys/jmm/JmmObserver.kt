package info.bagen.rust.plaoc.microService.sys.jmm

import androidx.compose.runtime.mutableStateMapOf
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.sys.jmm.ui.DownLoadStatus

typealias DownLoadChangObserver = (Mmid, DownLoadStatus) -> Unit

object DownLoadStatusSubject {
  private val listeners = mutableStateMapOf<DownLoadChangObserver, Mmid>()

  fun attach(mmid: Mmid, observer: DownLoadChangObserver) {
    listeners[observer] = mmid
  }

  fun detach(observer: DownLoadChangObserver) {
    listeners.remove(observer)
  }

  private fun detach(mmid: Mmid) {
    val iterator = listeners.iterator()
    iterator.forEach {
      if (it.value == mmid) iterator.remove()
    }
  }

  fun callObservers(mmid: Mmid, downLoadStatus: DownLoadStatus) {
    listeners.forEach { (key, value) ->
      if (value == mmid) { key(mmid, downLoadStatus) }
    }
  }
}