package org.dweb_browser.window.core

import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.android.BaseActivity
import org.dweb_browser.window.core.constant.WindowManagerPropertyKeys
import org.dweb_browser.window.render.LocalWindowController
import org.dweb_browser.window.render.LocalWindowPadding
import org.dweb_browser.window.render.LocalWindowsManager
import org.dweb_browser.window.render.watchedBounds

class ManagerState(val activity: BaseActivity) {
  val viewHeight get() = activity.window.decorView.height
  val viewWidth get() = activity.window.decorView.width
  val displayDensity get() = activity.resources.displayMetrics.density
  val viewHeightDp get() = viewHeight / displayDensity
  val viewWidthDp get() = viewWidth / displayDensity

  companion object {
    /// 一些提供给 WindowsManager 的扩展函数
    @Composable
    fun <T, W : WindowController> WindowsManager<W>.watchedState(
      key: Any? = null,
      policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy(),
      filter: ((change: Observable.Change<WindowManagerPropertyKeys, *>) -> Boolean)? = null,
      watchKey: WindowManagerPropertyKeys? = null,
      watchKeys: Set<WindowManagerPropertyKeys>? = null,
      getter: ManagerState.() -> T,
    ) = remember(key) {
      mutableStateOf(getter.invoke(state), policy)
    }.also { rememberState ->
      DisposableEffect(state) {
        val off = state.observable.onChange {
          if ((if (watchKey != null) watchKey == it.key else true) && (if (watchKeys != null) watchKeys.contains(
              it.key
            ) else true) && filter?.invoke(it) != false
          ) {
            rememberState.value = getter.invoke(state)
          }
        }
        onDispose {
          off()
        }
      }
    }

    @Composable
    fun <T : WindowController> WindowsManager<T>.watchedImeBounds() =
      watchedState(WindowManagerPropertyKeys.ImeBounds) { this.imeBounds }


    fun Modifier.windowImeOutsetBounds() = composed {
      composed {
        this
          .runCatching {
            val wsm = LocalWindowsManager.current;
            val imeVisible by wsm.watchedState { imeVisible }
            if (!imeVisible) {
              return@runCatching this.offset(y = 0.dp)
            }
            val win = LocalWindowController.current;
            val winBounds by win.watchedBounds()
            val imeBounds by wsm.watchedImeBounds()
            val winOuterY = winBounds.top + winBounds.height
            if (winOuterY <= imeBounds.top) {
              // 两个矩形没有交集，不需要进行任何 Modifier 修饰
              this.offset(y = 0.dp)
            }
            /// 尝试进行偏移修饰
            else {

              val offsetY = winOuterY - imeBounds.top
              // 窗口可以通过向上偏移来确保键盘与窗口同时显示
              if (offsetY <= winBounds.top) {
                return@runCatching this.offset(y = -offsetY.dp)
              }
              val winPadding = LocalWindowPadding.current
              val offsetY2 = offsetY - winPadding.bottom
              // 窗口可以牺牲 底部区域的显示
              if (offsetY2 <= winBounds.top) {
                return@runCatching this.offset(y = -winBounds.top.dp)
              }
              // 窗口底部区域始终不能妥协，一定会显示，这样才能确保 最大化模式下 的窗口可以复用这套逻辑

              // 计算出最终插入内容的底部高度
              win.state.keyboardInsetBottom = offsetY2 - winBounds.top
              this.offset(y = -winBounds.top.dp)
            }
          }
          .getOrDefault(this.offset(y = 0.dp))
      }
    }

    fun Modifier.windowImeInsetBounds() = this
  }

  /**
   * 以下是可变属性，所以这里提供一个监听器，来监听所有的属性变更
   */
  val observable = Observable<WindowManagerPropertyKeys>();

  /**
   * IME(input method editor 输入法) 的位置和大小
   */
  var imeBounds by observable.observe(
    WindowManagerPropertyKeys.ImeBounds, WindowBounds(0f, 0f, 0f, 0f)
  )

  var imeVisible by observable.observe(WindowManagerPropertyKeys.ImeVisible, false)

  init {
    val imeType = WindowInsetsCompat.Type.ime()
    val decorView = activity.window.decorView
    decorView.viewTreeObserver.addOnGlobalLayoutListener {
      val windowInsetsCompat = WindowInsetsCompat.toWindowInsetsCompat(decorView.rootWindowInsets)
      val imeVisible = windowInsetsCompat.isVisible(imeType)
      this.imeVisible = imeVisible
      this.imeBounds = if (imeVisible) {
        val imeInsets = windowInsetsCompat.getInsets(imeType)
        val imeHeightDp = imeInsets.bottom / displayDensity
        WindowBounds(
          left = 0f,
          top = viewHeightDp - imeHeightDp,
          height = imeHeightDp,
          width = viewWidthDp,
        )
      } else {
        WindowBounds.Zero
      }

      // 输入法高度即为 heightDiff
      println("imeBounds:$imeBounds, imeVisible:$imeVisible")
    }
  }

}