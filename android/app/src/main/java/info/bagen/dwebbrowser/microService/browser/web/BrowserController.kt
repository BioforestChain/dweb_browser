package info.bagen.dwebbrowser.microService.browser.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dweb_browser.browserUI.ui.browser.BrowserViewModel
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.microservice.sys.http.HttpDwebServer

class BrowserController(
  private val browserNMM: BrowserNMM,
  private val browserServer: HttpDwebServer
) {

  internal val updateSignal = SimpleSignal()
  val onUpdate = updateSignal.toListener()

  val showLoading: MutableState<Boolean> = mutableStateOf(false)
  val browserViewModel by lazy {
    BrowserViewModel(browserNMM,browserServer) { mmid ->
      activity?.lifecycleScope?.launch {
        browserNMM.bootstrapContext.dns.open(mmid)
      }
    }
  }

  private var activityTask = PromiseOut<BrowserActivity>()
  suspend fun waitActivityCreated() = activityTask.waitPromise()

  var activity: BrowserActivity? = null
    set(value) {
      if (field == value) {
        return
      }
      field = value
      if (value == null) {
        activityTask = PromiseOut()
      } else {
        activityTask.resolve(value)
      }
    }

  val currentInsets: MutableState<WindowInsetsCompat> by lazy {
    mutableStateOf(
      WindowInsetsCompat.toWindowInsetsCompat(
        activity!!.window.decorView.rootWindowInsets
      )
    )
  }

  @Composable
  fun effect(activity: BrowserActivity): BrowserController {
    /**
     * 这个 NativeUI 的逻辑是工作在全屏幕下，所以会使得默认覆盖 系统 UI
     */
    SideEffect {
      WindowCompat.setDecorFitsSystemWindows(activity.window, false)
      /// system-bar 一旦隐藏（visible = false），那么被手势划出来后，过一会儿自动回去
      //windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

      ViewCompat.setOnApplyWindowInsetsListener(activity.window.decorView) { _, insets ->
        currentInsets.value = insets
        insets
      }

    }
    return this
  }
}