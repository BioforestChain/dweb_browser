package org.dweb_browser.sys.window.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.sys.window.core.constant.WindowManagerPropertyKeys

class WindowsManagerState(
  val viewBox: IPureViewBox,
) {
  //  val viewHeight get() = activity.window.decorView.height
//  val viewWidth get() = activity.window.decorView.width
//  val displayDensity get() = activity.resources.displayMetrics.density
  suspend fun viewSize() = viewBox.getViewSize()

  companion object {
    /// 一些提供给 WindowsManager 的扩展函数
    @Composable
    fun <T, W : WindowController> WindowsManager<W>.watchedState(
      key: Any? = null,
      policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy(),
      filter: ((change: Observable.Change<WindowManagerPropertyKeys, *>) -> Boolean)? = null,
      watchKey: WindowManagerPropertyKeys? = null,
      watchKeys: Set<WindowManagerPropertyKeys>? = null,
      getter: WindowsManagerState.() -> T,
    ): State<T> = remember(key) {
      val rememberState = mutableStateOf(getter.invoke(state), policy)
      val off = state.observable.onChange {
        if ((if (watchKey != null) watchKey == it.key else true) && (if (watchKeys != null) watchKeys.contains(
            it.key
          ) else true) && filter?.invoke(it) != false
        ) {
          rememberState.value = getter.invoke(state)
        }
      }
      Pair(rememberState, off)
    }.let { (rememberState, off) ->
      DisposableEffect(off) {
        onDispose {
          off()
        }
      }
      rememberState
    }

  }

  /**
   * 以下是可变属性，所以这里提供一个监听器，来监听所有的属性变更
   */
  val observable = Observable<WindowManagerPropertyKeys>()

  /**
   * IME(input method editor 输入法) 的位置和大小
   */
  var imeBoundingRect by observable.observe(
    WindowManagerPropertyKeys.ImeBoundingRect, PureRect(0f, 0f, 0f, 0f)
  )

  /**
   * IME 是否可见
   */
  var imeVisible by observable.observe(WindowManagerPropertyKeys.ImeVisible, false)

  /**
   * safeMode 用于桌面的处理
   */
  var safeMode by observable.observe(WindowManagerPropertyKeys.SafeMode, false)
}