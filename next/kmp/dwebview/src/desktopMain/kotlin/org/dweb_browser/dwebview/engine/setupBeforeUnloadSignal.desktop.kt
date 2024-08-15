package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.browser.callback.BeforeUnloadCallback
import com.teamdev.jxbrowser.browser.event.ConsoleMessageReceived
import com.teamdev.jxbrowser.event.Observer
import com.teamdev.jxbrowser.event.Subscription
import com.teamdev.jxbrowser.js.ConsoleMessageLevel
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.WebBeforeUnloadArgs
import org.dweb_browser.dwebview.WebLoadSuccessState
import org.dweb_browser.helper.Signal

const val NoDialogNoUserGesture =
  "Blocked attempt to show a 'beforeunload' confirmation panel for a frame that never had a user gesture since its load. https://www.chromestatus.com/feature/5082396709879808"

fun setupBeforeUnloadSignal(engine: DWebViewEngine) = Signal<WebBeforeUnloadArgs>().also { signal ->
  val consoleObserver = Observer<ConsoleMessageReceived> { event ->
    val consoleMessage = event.consoleMessage()
    // 需要用户手势参与才能触发 beforeunload， https://www.chromestatus.com/feature/5082396709879808
    if (consoleMessage.level() == ConsoleMessageLevel.LEVEL_ERROR && consoleMessage.message() == NoDialogNoUserGesture) {
      engine.lifecycleScope.launch {
        val args = WebBeforeUnloadArgs(NoDialogNoUserGesture)
        signal.emit(args)
      }
    }
  }
  val sub = atomic<Subscription?>(null)
  signal.whenNoEmpty {
    sub.update {
      it ?: engine.browser.on(ConsoleMessageReceived::class.java, consoleObserver)
    }
  }
  signal.whenEmpty {
    sub.update {
      it?.unsubscribe()
      null
    }
  }
  engine.browser.on(ConsoleMessageReceived::class.java) { event ->
    val consoleMessage = event.consoleMessage()
    // 需要用户手势参与才能触发 beforeunload， https://www.chromestatus.com/feature/5082396709879808
    if (consoleMessage.level() == ConsoleMessageLevel.LEVEL_ERROR && consoleMessage.message() == NoDialogNoUserGesture) {

    }
  }
  engine.browser.set(BeforeUnloadCallback::class.java, BeforeUnloadCallback { params, tell ->
    engine.lifecycleScope.launch {
      var isKeep = false
      if (signal.isNotEmpty()) {
        val args = WebBeforeUnloadArgs(
          params.message(),
          params.title(),
          params.leaveActionText(),
          params.stayActionText(),
          params.isReload
        )

        signal.emit(args)
        isKeep = !args.waitHookResults()
      }
      when {
        isKeep -> {
          tell.stay()
          engine.loadStateFlow.emit(WebLoadSuccessState(engine.getOriginalUrl()))
        }

        else -> tell.leave()
      }
    }
  });
}