package info.bagen.dwebbrowser.microService.browser.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import info.bagen.dwebbrowser.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browserUI.ui.browser.BrowserViewModel
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.sys.http.HttpDwebServer
import org.dweb_browser.window.core.WindowController
import org.dweb_browser.window.core.WindowState
import org.dweb_browser.window.core.constant.WindowConstants
import org.dweb_browser.window.core.constant.WindowMode
import org.dweb_browser.window.core.createWindowAdapterManager

class BrowserController(
    private val browserNMM: BrowserNMM,
    browserServer: HttpDwebServer
) {

    internal val updateSignal = SimpleSignal()
    val onUpdate = updateSignal.toListener()


    private var winLock = Mutex(false)

    suspend fun uninstallWindow() {
        winLock.withLock {
            win?.close(false)
        }
    }

    /**
     * 窗口是单例模式
     */
    private var win: WindowController? = null
    suspend fun openBrowserWindow(ipc: Ipc, search: String? = null, url: String? = null) =
        winLock.withLock<WindowController> {
            // 打开安装窗口
            val win = createWindowAdapterManager.createWindow(
                WindowState(
                    WindowConstants(
                        owner = ipc.remote.mmid,
                        provider = browserNMM.mmid,
                        microModule = browserNMM
                    )
                ).also {
                    it.mode = WindowMode.MAXIMIZE
                })
            win.state.closeTip =
                win.manager?.state?.activity?.resources?.getString(R.string.browser_confirm_to_close)
                    ?: ""
            this.win = win
            val wid = win.id
            /// 提供渲染适配
            createWindowAdapterManager.renderProviders[wid] =
                @Composable { modifier ->
                    Render(modifier, this)
                }
            /// 窗口销毁的时候
            win.onClose {
                // 移除渲染适配器
                createWindowAdapterManager.renderProviders.remove(wid)
                ioAsyncScope.cancel()
            }
            return win
        }

    private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler
    val showLoading: MutableState<Boolean> = mutableStateOf(false)
    val viewModel = BrowserViewModel(browserNMM, browserServer) { mmid ->
        ioAsyncScope.launch {
            browserNMM.bootstrapContext.dns.open(mmid)
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