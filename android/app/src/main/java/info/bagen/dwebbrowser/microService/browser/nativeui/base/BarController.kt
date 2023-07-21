package info.bagen.dwebbrowser.microService.browser.nativeui.base


import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import info.bagen.dwebbrowser.microService.sys.helper.ColorJson
import info.bagen.dwebbrowser.microService.browser.nativeui.NativeUiController
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.BarStyle
import info.bagen.dwebbrowser.util.IsChange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.PipedInputStream
import java.io.PipedOutputStream


abstract class BarController(
  activity: ComponentActivity,
  nativeUiController: NativeUiController,
) : InsetsController(activity, nativeUiController) {

    val inputStream = PipedInputStream()
    val outputStream = PipedOutputStream()

    init {
        outputStream.connect(inputStream)
    }

    /**
     * 背景色
     */
    val colorState = mutableStateOf(Color.Transparent)

    /**
     * 前景风格
     */
    val styleState = mutableStateOf(BarStyle.Default)

    /**
     * 是否可见
     */
    val visibleState = mutableStateOf(true)

    @Composable
    protected open override fun observerWatchStates(stateChanges: IsChange) {

        super.observerWatchStates(stateChanges)
        stateChanges.rememberByState(colorState)
        stateChanges.rememberByState(styleState)
        stateChanges.rememberByState(visibleState)
    }

    @Composable
    abstract override fun effect(): BarController

    interface BarState : InsetsState {
        val visible: Boolean
        val style: BarStyle
        val color: ColorJson
    }

    abstract override fun toJsonAble(): BarState
}
