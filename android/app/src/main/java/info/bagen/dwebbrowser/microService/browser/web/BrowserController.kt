package info.bagen.dwebbrowser.microService.browser.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import info.bagen.dwebbrowser.microService.core.WindowController
import info.bagen.dwebbrowser.microService.core.windowAdapterManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dweb_browser.browserUI.ui.browser.BrowserViewModel
import org.dweb_browser.microservice.sys.http.HttpDwebServer

class BrowserController(
  val win: WindowController, // 窗口控制器
  browserNMM: BrowserNMM,
  browserServer: HttpDwebServer
) {
  val showLoading: MutableState<Boolean> = mutableStateOf(false)
  val viewModel = BrowserViewModel(browserNMM, browserServer) { mmid ->
    @OptIn(DelicateCoroutinesApi::class)
    GlobalScope.launch {
      browserNMM.bootstrapContext.dns.open(mmid)
    }
  }

  init {
    val wid = win.id
    /// 提供渲染适配
    windowAdapterManager.providers[wid] =
      @Composable { modifier, width, height, scale ->
        Render(modifier,width, height, scale)
      }
    /// 窗口销毁的时候
    win.onClose{
      // 移除渲染适配器
      windowAdapterManager.providers.remove(wid)
    }
  }

  internal fun updateDWSearch(search: String) = viewModel.setDwebLinkSearch(search)
  internal fun updateDWUrl(url: String) = viewModel.setDwebLinkUrl(url)


//  val browserViewModel by lazy {
//    BrowserViewModel(browserNMM,browserServer) { mmid ->
//      activity?.lifecycleScope?.launch {
//        browserNMM.bootstrapContext.dns.open(mmid)
//      }
//    }
//  }
//
//  private var activityTask = PromiseOut<BrowserActivity>()
//  suspend fun waitActivityCreated() = activityTask.waitPromise()
//
//  var activity: BrowserActivity? = null
//    set(value) {
//      if (field == value) {
//        return
//      }
//      field = value
//      if (value == null) {
//        activityTask = PromiseOut()
//      } else {
//        activityTask.resolve(value)
//      }
//    }
//
//  val currentInsets: MutableState<WindowInsetsCompat> by lazy {
//    mutableStateOf(
//      WindowInsetsCompat.toWindowInsetsCompat(
//        activity!!.window.decorView.rootWindowInsets
//      )
//    )
//  }
//
//  @Composable
//  fun effect(activity: BrowserActivity): BrowserController {
//    /**
//     * 这个 NativeUI 的逻辑是工作在全屏幕下，所以会使得默认覆盖 系统 UI
//     */
//    SideEffect {
//      WindowCompat.setDecorFitsSystemWindows(activity.window, false)
//      /// system-bar 一旦隐藏（visible = false），那么被手势划出来后，过一会儿自动回去
//      //windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//
//      ViewCompat.setOnApplyWindowInsetsListener(activity.window.decorView) { _, insets ->
//        currentInsets.value = insets
//        insets
//      }
//
//    }
//    return this
//  }
}