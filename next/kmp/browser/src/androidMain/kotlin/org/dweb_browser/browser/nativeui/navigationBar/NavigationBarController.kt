package org.dweb_browser.browser.nativeui.navigationBar


import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowInsetsCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import org.dweb_browser.helper.android.InsetsJson
import org.dweb_browser.helper.android.toJsonAble
import org.dweb_browser.browser.nativeui.helper.debugNativeUi
import org.dweb_browser.browser.nativeui.helper.toWindowsInsets
import org.dweb_browser.helper.compose.ColorJson
import org.dweb_browser.helper.compose.toJsonAble


@Stable
class NavigationBarController(
  activity: ComponentActivity,
  nativeUiController: org.dweb_browser.browser.nativeui.NativeUiController,
) : org.dweb_browser.browser.nativeui.base.BarController(activity, nativeUiController) {

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
          org.dweb_browser.browser.nativeui.helper.BarStyle.Dark -> true
          org.dweb_browser.browser.nativeui.helper.BarStyle.Light -> false
          else -> color.luminance() > 0.5F
        },
      )
      onDispose { }
    }

    observer.stateChanges.also {
      observerWatchStates(it)

      it.HandleChange {
        debugNativeUi("NavigationBar", "CHANGED")
        LaunchedEffect(Unit){
          observer.notifyObserver()
        }
      }
    }

    return this
  }


  data class NavigationBarState(
    override val visible: Boolean,
    override val style: org.dweb_browser.browser.nativeui.helper.BarStyle,
    override val overlay: Boolean,
    override val color: ColorJson,
    override val insets: InsetsJson,
  ) : BarState

  override fun toJsonAble() = NavigationBarState(
    visible = visibleState.value,
    style = styleState.value,
    overlay = overlayState.value,
    color = colorState.value.toJsonAble(),
    insets = insetsState.value.toJsonAble(activity),
  )
}
