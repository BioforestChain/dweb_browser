package org.dweb_browser.window.core

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.platform.PlatformViewController
import org.dweb_browser.window.core.constant.WindowManagerPropertyKeys
import org.dweb_browser.window.core.constant.debugWindow
import org.dweb_browser.window.render.LocalWindowController
import org.dweb_browser.window.render.LocalWindowPadding
import org.dweb_browser.window.render.LocalWindowsImeVisible
import org.dweb_browser.window.render.LocalWindowsManager
import org.dweb_browser.window.render.watchedBounds
import kotlin.math.max

class ManagerState(
  val viewController: PlatformViewController,
) {
//  val viewHeight get() = activity.window.decorView.height
//  val viewWidth get() = activity.window.decorView.width
//  val displayDensity get() = activity.resources.displayMetrics.density

  val viewHeightDp get() = with(viewController) { getViewHeightPx() / getDisplayDensity() }
  val viewWidthDp get() = with(viewController) { getViewWidthPx() / getDisplayDensity() }

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

    @Composable
    fun <T : WindowController> WindowsManager<T>.watchedImeBounds() =
      watchedState(WindowManagerPropertyKeys.ImeBoundingRect) { this.imeBoundingRect }

    fun Modifier.windowImeOutsetBounds() = composed {
      composed {
        this
          .runCatching {
            val wsm = LocalWindowsManager.current
            val win = LocalWindowController.current
            val imeVisible by wsm.watchedState { imeVisible }
            val modifierOffsetY: Float
            val keyboardInsetBottom: Float
            // 键盘不显示
            if (!imeVisible) {
              modifierOffsetY = 0f
              keyboardInsetBottom = 0f
            } else {
              val winBounds by win.watchedBounds()
              val imeBounds by wsm.watchedImeBounds()
              val winOuterY = winBounds.y + winBounds.height

              if (winOuterY <= imeBounds.y) {
                // 两个矩形没有交集
                modifierOffsetY = 0f
                keyboardInsetBottom = 0f
              }
              /// 尝试进行偏移修饰
              else {
                val offsetY = winOuterY - imeBounds.y
                // 窗口可以通过向上偏移来确保键盘与窗口同时显示
                if (offsetY <= winBounds.y) {
                  modifierOffsetY = -offsetY
                  keyboardInsetBottom = 0f
                } else {
                  modifierOffsetY = -winBounds.y
                  val winPadding = LocalWindowPadding.current
                  val offsetY2 = offsetY - winPadding.bottom
                  // 窗口可以牺牲底部区域的显示，多出来的就是键盘的插入高度
                  keyboardInsetBottom = max(offsetY2 - winBounds.y, 0f)
                }
              }
            }
            win.state.keyboardInsetBottom = keyboardInsetBottom
            this.offset(y = modifierOffsetY.dp)
          }
          .getOrDefault(this)
      }
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
    WindowManagerPropertyKeys.ImeBoundingRect, Rect.Zero
  )

  var imeVisible by observable.observe(WindowManagerPropertyKeys.ImeVisible, false)

  /**
   * 专门用来监听键盘的变化，然后修改WindowBounds的宽高等
   */
  @OptIn(ExperimentalLayoutApi::class)
  @Composable
  fun EffectKeyboardStates() {
    imeVisible = LocalWindowsImeVisible.current.value // WindowInsets.isImeVisible // 由于小米手机的安全键盘存在问题，会出现WindowInsets.isImeVisible不正确的情况
    val ime = WindowInsets.imeAnimationTarget // 直接使用ime，数据不稳定，会变化，改为imeAnimationTarget就是固定值
    val density = LocalDensity.current
    val view = LocalView.current

    LaunchedEffect(view, imeVisible) { // WindowInsets.ime 对象并不会变化，所以导致这个重组不会重复执行
      this@ManagerState.imeBoundingRect = if (imeVisible) {
        val imeHeightDp = ime.getBottom(density) / density.density
        Rect(
          x = 0f,
          y = view.height / density.density - imeHeightDp,
          height = imeHeightDp,
          width = view.width / density.density,
        )
      } else {
        Rect.Zero
      }
      // 输入法高度即为 heightDiff
      debugWindow("ManagerState/IME", "imeVisible:$imeVisible, imeBoundingRect:$imeBoundingRect")
    }
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//      val scope = rememberCoroutineScope()
//      DisposableEffect(view) {
//        var lastJob = scope.launch { }
//        val off = view.addWindowInsetsAnimationListener { insets, runningAnimations ->
//          val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
//          lastJob.cancel()
//          //debugWindow("ManagerState/IME", "imeInsets.bottom=${imeInsets.bottom}")
//          lastJob = scope.launch {
//            delay(100);
//            imeVisible = imeInsets.bottom != 0
//          }
//          insets
//        }
//        onDispose {
//          off();
//        }
//      }
//    }
  }
}