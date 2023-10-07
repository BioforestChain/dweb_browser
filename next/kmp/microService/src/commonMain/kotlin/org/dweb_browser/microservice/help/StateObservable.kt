package org.dweb_browser.microservice.help

import io.ktor.utils.io.core.toByteArray
import org.dweb_browser.helper.compose.IsChange
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.Callback
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.microservice.http.PureStream
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.ReadableStream

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
          controller.close()
          e.printStackTrace()
        }
      }
      ipc.onClose {
        off()
        controller.close()
      }
    }.stream
  }

  fun notifyObserver() {
    runBlockingCatching {
      changeSignal.emit(getStateJson())
    }.getOrNull()
  }
}