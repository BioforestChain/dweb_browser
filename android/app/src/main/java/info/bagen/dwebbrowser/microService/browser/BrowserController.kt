package info.bagen.dwebbrowser.microService.browser

import android.view.View
import android.view.ViewGroup
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
import org.dweb_browser.helper.*

class BrowserController(val browserNMM: BrowserNMM) {
    val showLoading: MutableState<Boolean> = mutableStateOf(false)
    val browserViewModel by lazy {
        BrowserViewModel(browserNMM) { mmid ->
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

    private val dWebViewList = mutableListOf<View>()

    fun appendView(view: View) = dWebViewList.add(view)
    val hasDwebView get() = dWebViewList.size > 0

    fun removeLastView(): Boolean {
        try {
            dWebViewList.removeLast().also { childView ->
                activity?.window?.decorView?.let { parentView ->
                    (parentView as ViewGroup).removeView(childView)
                }
            }
        } catch (e: NoSuchElementException) {
            return false
        }
        return true
    }
}