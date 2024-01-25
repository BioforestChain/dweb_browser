package org.dweb_browser.core.help

import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.helper.ReadableStream
import org.dweb_browser.helper.Callback
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.compose.IsChange
import org.dweb_browser.pure.http.PureStream

open class StateObservable(
  needFirstCall: Boolean = false,
  private val getStateJson: () -> String,
) {
  val stateChanges = IsChange(needFirstCall)
  private val changeSignal = Signal<String>()
  fun observe(cb: Callback<String>) = changeSignal.listen(cb)

  suspend fun startObserve(ipc: Ipc): PureStream {
    return ReadableStream { controller ->
      val off = observe { state ->
        try {
          controller.enqueueBackground((Json.encodeToString(state) + "\n").toByteArray())
        } catch (e: Exception) {
          controller.closeWrite()
          e.printStackTrace()
        }
      }
      ipc.onClose {
        off()
        controller.closeWrite()
      }
    }.stream
  }

  suspend fun notifyObserver() {
    changeSignal.emit(getStateJson())
  }
}