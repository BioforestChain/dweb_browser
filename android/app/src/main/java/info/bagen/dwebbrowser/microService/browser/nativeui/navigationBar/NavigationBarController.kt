package info.bagen.dwebbrowser.microService.browser.nativeui.navigationBar


import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowInsetsCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import info.bagen.dwebbrowser.microService.sys.helper.ColorJson
import info.bagen.dwebbrowser.microService.sys.helper.InsetsJson
import info.bagen.dwebbrowser.microService.sys.helper.toJsonAble
import info.bagen.dwebbrowser.microService.browser.nativeui.NativeUiController
import info.bagen.dwebbrowser.microService.browser.nativeui.base.BarController
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.BarStyle
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.debugNativeUi
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.toWindowsInsets


@Stable
class NavigationBarController(
  activity: ComponentActivity,
  nativeUiController: NativeUiController,
) : BarController(activity, nativeUiController) {

    @Composable
    override fun effect(): NavigationBarController {
        val systemUiController = rememberSystemUiController()

        insetsState.value =
            nativeUiController.getCurrentInsets(WindowInsetsCompat.Type.navigationBars())
                .toWindowsInsets()

        val color by colorState
        val style by styleState
        var visible by visibleState
        DisposableEffect(visible, color, style) {
            debugNativeUi(
                "DisposableEffect", "visible:$visible; color:$color; style:$style"
            )
            systemUiController.isNavigationBarVisible = visible
            systemUiController.setNavigationBarColor(
                color = color,
                darkIcons = when (style) {
                    BarStyle.Dark -> true
                    BarStyle.Light -> false
                    else -> color.luminance() > 0.5F
                },
            )
            onDispose { }
        }

        observer.stateChanges.also {
            observerWatchStates(it)

            it.effectChange {
                debugNativeUi("NavigationBar", "CHANGED")
                observer.notifyObserver()
            }
        }

        return this
    }


    data class NavigationBarState(
      override val visible: Boolean,
      override val style: BarStyle,
      override val overlay: Boolean,
      override val color: ColorJson,
      override val insets: InsetsJson,
    ) : BarState

    override fun toJsonAble() = NavigationBarState(
        visible = visibleState.value,
        style = styleState.value,
        overlay = overlayState.value,
        color = colorState.value.toJsonAble(),
        insets = insetsState.value.toJsonAble(),
    )
}
