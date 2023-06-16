package info.bagen.dwebbrowser.microService.browser.nativeui.virtualKeyboard


import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.view.*
import com.google.gson.*
import info.bagen.dwebbrowser.microService.sys.helper.InsetsJson
import info.bagen.dwebbrowser.microService.sys.helper.toJsonAble
import info.bagen.dwebbrowser.microService.helper.*
import info.bagen.dwebbrowser.microService.browser.nativeui.NativeUiController
import info.bagen.dwebbrowser.microService.browser.nativeui.base.InsetsController
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.toWindowsInsets

@Stable
class VirtualKeyboardController(
  activity: ComponentActivity,
  nativeUiController: NativeUiController,
) : InsetsController(activity, nativeUiController) {

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
        insets = insetsState.value.toJsonAble(),
    )
}
