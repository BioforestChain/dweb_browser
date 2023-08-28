package info.bagen.dwebbrowser.helper

import info.bagen.dwebbrowser.util.IsChange
import org.dweb_browser.helper.Callback
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.microservice.help.gson
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.ReadableStream
import java.io.InputStream

open class StateObservable(
  needFirstCall: Boolean = false,
  private val getStateJson: () -> String,
) {
  val stateChanges = IsChange(needFirstCall)
  private val changeSignal = Signal<String>()
  fun observe(cb: Callback<String>) = changeSignal.listen(cb)

  suspend fun startObserve(ipc: Ipc): InputStream {
    return ReadableStream(onStart = { controller ->
      val off = observe { state ->
        try {
          controller.enqueueBackground((gson.toJson(state) + "\n").toByteArray())
        } catch (e: Exception) {
          controller.close()
          e.printStackTrace()
        }
      }
      ipc.onClose {
        off()
        controller.close()
      }
    })
  }

  fun notifyObserver() {
    runBlockingCatching {
      changeSignal.emit(getStateJson())
    }.getOrNull()
  }
}