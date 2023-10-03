package org.dweb_browser.browser.nativeui.base


import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import org.dweb_browser.browser.nativeui.NativeUiController
import org.dweb_browser.browser.nativeui.helper.BarStyle
import org.dweb_browser.helper.compose.IsChange
import org.dweb_browser.helper.compose.ColorJson


abstract class BarController(
  activity: ComponentActivity,
  nativeUiController: org.dweb_browser.browser.nativeui.NativeUiController,
) : org.dweb_browser.browser.nativeui.base.InsetsController(activity, nativeUiController) {

  /**
   * 背景色
   */
  val colorState = mutableStateOf(Color.Transparent)

  /**
   * 前景风格
   */
  val styleState = mutableStateOf(org.dweb_browser.browser.nativeui.helper.BarStyle.Default)

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
  abstract override fun effect(): org.dweb_browser.browser.nativeui.base.BarController

  interface BarState : org.dweb_browser.browser.nativeui.base.InsetsController.InsetsState {
    val visible: Boolean
    val style: org.dweb_browser.browser.nativeui.helper.BarStyle
    val color: ColorJson
  }

  abstract override fun toJsonAble(): org.dweb_browser.browser.nativeui.base.BarController.BarState
}
