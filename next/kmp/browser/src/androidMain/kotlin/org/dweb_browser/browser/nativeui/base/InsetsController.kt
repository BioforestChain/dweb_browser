package org.dweb_browser.browser.nativeui.base

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import org.dweb_browser.helper.android.InsetsJson
import org.dweb_browser.microservice.help.StateObservable
import org.dweb_browser.browser.nativeui.NativeUiController
import org.dweb_browser.helper.compose.IsChange
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

abstract class InsetsController(
  val activity: ComponentActivity,
  val nativeUiController: org.dweb_browser.browser.nativeui.NativeUiController,
) {
  /**
   * 是否层叠渲染
   */
  val overlayState = mutableStateOf(true)

  /**
   * 插入空间
   */
  val insetsState = mutableStateOf(WindowInsets(0))

  /**
   * 状态监听器
   */
  val observer = StateObservable { Json.encodeToString(toJsonAble()) }

  @Composable
  protected open fun observerWatchStates(stateChanges: IsChange) {
    stateChanges.rememberByState(overlayState)
    stateChanges.rememberByState(insetsState)
  }

  @Composable
  abstract fun effect(): org.dweb_browser.browser.nativeui.base.InsetsController

  interface InsetsState {
    val overlay: Boolean
    val insets: InsetsJson
  }

  abstract fun toJsonAble(): org.dweb_browser.browser.nativeui.base.InsetsController.InsetsState
}