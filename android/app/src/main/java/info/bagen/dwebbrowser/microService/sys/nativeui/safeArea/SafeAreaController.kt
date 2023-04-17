package info.bagen.dwebbrowser.microService.sys.nativeui.safeArea


import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import info.bagen.dwebbrowser.microService.helper.InsetsJson
import info.bagen.dwebbrowser.microService.helper.toJsonAble
import info.bagen.dwebbrowser.microService.sys.nativeui.NativeUiController
import info.bagen.dwebbrowser.microService.sys.nativeui.base.InsetsController
import info.bagen.dwebbrowser.microService.sys.nativeui.helper.debugNativeUi
import info.bagen.dwebbrowser.microService.sys.nativeui.helper.plus

/**
 * 安全区域，被 设备的顶部流海、状态栏、导航栏等原生UI所影响后，分割出inner、outer两个部分
 */
class SafeAreaController(
    activity: ComponentActivity, nativeUiController: NativeUiController
) : InsetsController(activity, nativeUiController) {
    /**
     * 刘海屏
     */
    val cutoutInsetsState = mutableStateOf(WindowInsets(0))


    /**
     * 外部区域
     */
    val outerAreaInsetsState = mutableStateOf(WindowInsets(0))


    @Composable
    override fun effect(): SafeAreaController {
        cutoutInsetsState.value = WindowInsets.Companion.displayCutout

        val statusBar = nativeUiController.statusBar
        val virtualKeyboard = nativeUiController.virtualKeyboard
        val navigationBar = nativeUiController.navigationBar
        val safeArea = this

        observer.stateChanges.let {
            val isSafeAreaOverlay by it.rememberByState(safeArea.overlayState)
            val safeAreaCutoutInsets by it.rememberByState(safeArea.cutoutInsetsState)

            val isStatusBarOverlay by it.rememberByState(statusBar.overlayState)
            val statusBarsInsets by it.rememberByState(statusBar.insetsState)

            val isVirtualKeyboardOverlay by it.rememberByState(virtualKeyboard.overlayState)
            val imeInsets by it.rememberByState(virtualKeyboard.insetsState)

            val isNavigationBarOverlay by it.rememberByState(navigationBar.overlayState)
            val navigationBarsInsets by it.rememberByState(navigationBar.insetsState)

            it.effectChange {
                var RES_safeAreaInsets = WindowInsets(0)
                var RES_outerAreaArea = WindowInsets(0)

                /// 顶部，顶部有状态栏和刘海区域
                val topInsets = statusBarsInsets.union(safeAreaCutoutInsets)
                if (isStatusBarOverlay && isSafeAreaOverlay) {
                    // 都覆盖，那么就写入safeArea，outerArea不需要调整
                    RES_safeAreaInsets += topInsets
                } else if (isStatusBarOverlay && !isSafeAreaOverlay) {
                    // outerArea写入刘海区域，safeArea只写剩余的
                    RES_outerAreaArea += safeAreaCutoutInsets
                    RES_safeAreaInsets += topInsets.exclude(safeAreaCutoutInsets)
                } else if (!isStatusBarOverlay && isSafeAreaOverlay) {
                    // outerArea写入状态栏，safeArea只写剩余的
                    RES_outerAreaArea += statusBarsInsets
                    RES_safeAreaInsets += topInsets.exclude(statusBarsInsets)
                } else {
                    // 都不覆盖，全部写入 outerArea
                    RES_outerAreaArea += topInsets
                }
                /// 底部，底部有导航栏和虚拟键盘
                val bottomInsets = navigationBarsInsets.union(imeInsets)
                if (isVirtualKeyboardOverlay && isNavigationBarOverlay) {
                    // 都覆盖，那么就写入safeArea，outerArea不需要调整
                    RES_safeAreaInsets += bottomInsets
                } else if (isVirtualKeyboardOverlay && !isNavigationBarOverlay) {
                    // outerArea写入导航栏，safeArea只写剩余的
                    RES_outerAreaArea += navigationBarsInsets
                    RES_safeAreaInsets += bottomInsets.exclude(navigationBarsInsets)
                } else if (!isVirtualKeyboardOverlay && isNavigationBarOverlay) {
                    // outerArea写入虚拟键盘，safeArea只写剩余的
                    RES_outerAreaArea += imeInsets
                    RES_safeAreaInsets += bottomInsets.exclude(imeInsets)
                } else if (!isVirtualKeyboardOverlay && !isNavigationBarOverlay) {
                    // 都不覆盖，全部写入 outerArea
                    RES_outerAreaArea += bottomInsets
                }

                insetsState.value = RES_safeAreaInsets
                outerAreaInsetsState.value = RES_outerAreaArea
                debugNativeUi("SafeArea", "CHANGED")
                observer.notifyObserver()
            }
        }
        return this
    }


    data class SafeAreaState(
      val cutoutInsets: InsetsJson,
      val outerInsets: InsetsJson,
      override val overlay: Boolean,
      override val insets: InsetsJson,
    ) : InsetsState

    override fun toJsonAble() = SafeAreaState(
        cutoutInsets = cutoutInsetsState.value.toJsonAble(),
        overlay = overlayState.value,
        outerInsets = outerAreaInsetsState.value.toJsonAble(),
        insets = insetsState.value.toJsonAble(),
    )
}