package org.dweb_browser.browser.nativeui.virtualKeyboard

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.view.WindowInsetsCompat
import org.dweb_browser.helper.android.InsetsJson
import org.dweb_browser.helper.android.toJsonAble
import org.dweb_browser.browser.nativeui.NativeUiController
import org.dweb_browser.browser.nativeui.base.InsetsController
import org.dweb_browser.browser.nativeui.helper.toWindowsInsets

@Stable
class VirtualKeyboardController(
  activity: ComponentActivity,
  nativeUiController: org.dweb_browser.browser.nativeui.NativeUiController,
) : org.dweb_browser.browser.nativeui.base.InsetsController(activity, nativeUiController) {

  val focusRequester = FocusRequester()

  /**
   * 是否显示
   */
  val visibleState = mutableStateOf(false)

  @OptIn(ExperimentalComposeUiApi::class)
  @Composable
  override fun effect(): VirtualKeyboardController {
    insetsState.value =
      nativeUiController.currentInsets.value.getInsets(WindowInsetsCompat.Type.ime())
        .toWindowsInsets()

    val visible by visibleState
    LocalSoftwareKeyboardController.current?.also { keyboard ->
      if (visible) {
        focusRequester.requestFocus()
        keyboard.show()
      } else {
        keyboard.hide()
      }
    }

    observer.stateChanges.also {
      observerWatchStates(it)
      it.effectChange {
        observer.notifyObserver()
      }
    }

    return this
  }


  data class VirtualKeyboardState(
    val visible: Boolean,
    override val overlay: Boolean,
    override val insets: InsetsJson,
  ) : InsetsState


  override fun toJsonAble() = VirtualKeyboardState(
    visible = visibleState.value,
    overlay = overlayState.value,
    insets = insetsState.value.toJsonAble(activity),
  )
}
