package info.bagen.dwebbrowser.microService.sys.nativeui


import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import info.bagen.dwebbrowser.microService.sys.nativeui.helper.QueryHelper
import info.bagen.dwebbrowser.microService.sys.nativeui.navigationBar.NavigationBarController
import info.bagen.dwebbrowser.microService.sys.nativeui.safeArea.SafeAreaController
import info.bagen.dwebbrowser.microService.sys.nativeui.statusBar.StatusBarController
import info.bagen.dwebbrowser.microService.sys.nativeui.virtualKeyboard.VirtualKeyboardController

class NativeUiController(
    val activity: ComponentActivity,
) {
    val windowInsetsController by lazy {
        WindowCompat.getInsetsController(
            activity.window, activity.window.decorView
        )
    }
    val currentInsets =
        mutableStateOf(WindowInsetsCompat.toWindowInsetsCompat(activity.window.decorView.rootWindowInsets))

    fun getCurrentInsets(typeMask: Int) = currentInsets.value.getInsets(typeMask)

    val statusBar = StatusBarController(activity, this)
    val navigationBar = NavigationBarController(activity, this)
    val virtualKeyboard = VirtualKeyboardController(activity, this)

    val safeArea = SafeAreaController(activity, this)


    @Composable
    fun effect(): NativeUiController {
        /**
         * 这个 NativeUI 的逻辑是工作在全屏幕下，所以会使得默认覆盖 系统 UI
         */
        SideEffect {
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            /// system-bar 一旦隐藏（visible = false），那么被手势划出来后，过一会儿自动回去
            windowInsetsController.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            ViewCompat.setOnApplyWindowInsetsListener(activity.window.decorView) { _, insets ->
                currentInsets.value = insets
                insets
            }

        }
        statusBar.effect()
        navigationBar.effect()
        virtualKeyboard.effect()
        safeArea.effect()

        return this
    }


    companion object {
        init {
            QueryHelper.init() // 初始化
        }
    }

}


