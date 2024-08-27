package org.dweb_browser.helper.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.areNavigationBarsVisible
import androidx.compose.foundation.layout.areStatusBarsVisible
import androidx.compose.foundation.layout.areSystemBarsVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Switch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.compose.asPureBounds
import org.dweb_browser.helper.getOrPut
import squircleshape.CornerSmoothing
import squircleshape.SquircleShape

/**
 * TODO 使用 systemGestureExclusionRects 来避免操作系统原本的手势区域，避免冲突
 */
class InsetsController(val window: Window) {
  val controller: WindowInsetsControllerCompat =
    WindowCompat.getInsetsController(window, window.decorView)

  /**
   * 导航栏的背景颜色
   */
  var navigationBarColor
    get() = Color(window.navigationBarColor)
    set(value) {
      window.navigationBarColor = value.toArgb()
    }

  /**
   * 导航栏的前景颜色（图标颜色）
   * false 意味着图标颜色 为亮色（白色）
   * true 意味着图标颜色 为暗色（黑色）
   */
  var isAppearanceLightNavigationBars
    get() = controller.isAppearanceLightNavigationBars
    set(value) {
      controller.isAppearanceLightNavigationBars = value
    }

  /**
   * 是否要开启强制对比如，如果导航颜色不可见，会默认为导航栏提供一层背景色
   */
  var isNavigationBarContrastEnforced
    get() = when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> window.isNavigationBarContrastEnforced
      else -> false
    }
    set(value) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = value
      }
    }

  enum class NavigationBarsBehavior {
    None,
    Show,
    ShowBySwipe,
  }

  /**
   * 这里不提供 systemBarsBehavior 的接口，而是封装了 navigationBarsBehavior，由我们自己来控制导航栏的行为
   *
   * 事情要从 BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE 这个行为说起，这个行为有一个问题：如果用户滑出 导航栏，此时程序是完全监控不到导航栏被滑出的。
   * 从某种程度上来说，这会对一些人带来困扰，从而导致手势冲突。在此基础上，如果你想让你的程序更加的精致，处理更多的交互细节，这种默认行为就无法接受。
   *
   * 接下来，就是要知道 systemBarsBehavior 和 WindowInsetsControllerCompat.show/hide 接口其实是有一定交集的，这会令人困惑
   * 首先解释一下 systemBarsBehavior，它有三种值：BEHAVIOR_SHOW_BARS_BY_TOUCH、BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE、BEHAVIOR_SHOW_BARS_BY_SWIPE
   * 其中 BEHAVIOR_SHOW_BARS_BY_SWIPE == BEHAVIOR_DEFAULT
   * 其中 BEHAVIOR_SHOW_BARS_BY_SWIPE 已经废弃，直接使用 BEHAVIOR_DEFAULT 就好
   * 其中 BEHAVIOR_SHOW_BARS_BY_TOUCH 已经废弃，和 BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE 行为一样
   *
   * 当我们执行 hide 的时候，其实背后是两个行为：首先是改变了 systemBarsBehavior 为 BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE，紧接着隐藏了导航栏。
   * 所以如果网络上很多地方给的代码其实有误导性，想要把导航栏设置成 BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE 的效果，直接调用 hide 接口就够了。
   *
   * 进一步地，意味着单纯改变 systemBarsBehavior 其实并不会直接带来什么变化。
   *
   * 这里的 navigationBarsBehavior 就是利用以上的规律来做控制
   * Show 意味着直接调用 controller.show
   * ShowBySwipe 意味着调用 controller.hide + systemBarsBehavior=default，并且当导航栏显示一会儿之后，会自动再进行hide操作
   */
  var navigationBarsBehavior by mutableStateOf(NavigationBarsBehavior.Show)
  private val navigationBarsType = WindowInsetsCompat.Type.navigationBars()

  fun hideNavigationBars() {
    // 改成 BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE 是为了获得动画效果，navBarBottom 会是一个线性动画
    controller.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller.hide(navigationBarsType)
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
  }

  fun showNavigationBars() {
    // 改成 BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE 是为了获得动画效果，navBarBottom 会是一个线性动画
    controller.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller.show(navigationBarsType)
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
  }

  @OptIn(ExperimentalLayoutApi::class)
  @Composable
  fun Effect() {
    SideEffect {
      // 默认禁用 isNavigationBarContrastEnforced，避免带来困扰
      isNavigationBarContrastEnforced = false
    }
    val areNavigationBarsVisible = WindowInsets.areNavigationBarsVisible
    LaunchedEffect(navigationBarsBehavior, areNavigationBarsVisible) {
      when (navigationBarsBehavior) {
        NavigationBarsBehavior.None -> {}
        NavigationBarsBehavior.Show -> showNavigationBars()
        NavigationBarsBehavior.ShowBySwipe -> {
          if (areNavigationBarsVisible)
            delay(2000)
          hideNavigationBars()
        }
      }
    }
  }

  /**
   * 调试器，针对 controller 和 window 官方的接口提供的控制器
   */
  @OptIn(ExperimentalLayoutApi::class)
  @Composable
  fun DebugDemo(
    modifier: Modifier = Modifier,
  ) {
    val density = LocalDensity.current
    LazyColumn(
      modifier
        .zIndex(100f)
        .windowInsetsPadding(WindowInsets.systemBars)
        .wrapContentSize()
        .padding(8.dp)
        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
    ) {
      item("navigationBarColor") {
        Text("navigationBarColor")
        val colors = listOf(Color.Red, Color.Blue, Color.Transparent, Color.Green, Color.Magenta)
        var navigationBarColor by remember { mutableStateOf(navigationBarColor) }
        Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
          colors.forEach { color ->
            Box(
              modifier = Modifier
                .size(50.dp)
                .background(
                  color, SquircleShape(15.dp, CornerSmoothing.Small)
                )
                .border(2.dp, Color.Black, SquircleShape(15.dp, CornerSmoothing.Small))
                .clickable {
                  navigationBarColor = color
                }
            )
          }
        }
        LaunchedEffect(navigationBarColor) {
          window.navigationBarColor = navigationBarColor.toArgb()
        }
        HorizontalDivider()
      }
      item("isAppearanceLightNavigationBars") {
        Text("isAppearanceLightNavigationBars")
        var isAppearanceLightNavigationBars by remember { mutableStateOf(controller.isAppearanceLightNavigationBars) }
        Switch(
          checked = isAppearanceLightNavigationBars,
          onCheckedChange = { isAppearanceLightNavigationBars = it })
        LaunchedEffect(isAppearanceLightNavigationBars) {
          controller.isAppearanceLightNavigationBars =
            isAppearanceLightNavigationBars
        }
        HorizontalDivider()
        ///
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          Text("isNavigationBarContrastEnforced")
          var isNavigationBarContrastEnforced by remember { mutableStateOf(window.isNavigationBarContrastEnforced) }
          Switch(
            checked = isNavigationBarContrastEnforced,
            onCheckedChange = { isNavigationBarContrastEnforced = it })
          LaunchedEffect(isNavigationBarContrastEnforced) {
            window.isNavigationBarContrastEnforced =
              isNavigationBarContrastEnforced
          }
          HorizontalDivider()
        }
      }

      item("systemBarsBehavior") {
        Text("systemBarsBehavior")
        val behaviors = remember {
          mapOf(
            "BEHAVIOR_SHOW_BARS_BY_TOUCH" to WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH,
            "BEHAVIOR_DEFAULT" to WindowInsetsControllerCompat.BEHAVIOR_DEFAULT,
            "BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE" to WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
            "BEHAVIOR_SHOW_BARS_BY_SWIPE" to WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE,
          )
        }
        val systemBarsBehavior = controller.systemBarsBehavior
        SingleChoiceSegmentedButtonRow {
          behaviors.toList().forEachIndexed { index, (name, behavior) ->
            SegmentedButton(
              shape = SegmentedButtonDefaults.itemShape(index = index, count = behaviors.size),
              onClick = { controller.systemBarsBehavior = behavior },
              selected = systemBarsBehavior == behavior
            ) {
              Text(
                name.replace("BEHAVIOR_", "").replace("_", " ").lowercase(),
                fontSize = 9.sp,
              )
            }
          }
        }
        HorizontalDivider()
      }

      item("areNavigationBarsVisible") {
        Text("areNavigationBarsVisible")
        Switch(
          checked = WindowInsets.areNavigationBarsVisible,
          onCheckedChange = {
            val type = WindowInsetsCompat.Type.navigationBars()
            if (it) {
              controller.show(type)
            } else {
              controller.hide(type)
            }
          },
        )
        val navigationBars = WindowInsets.navigationBars
        val navigationBarsIgnoringVisibility = WindowInsets.navigationBarsIgnoringVisibility
        Text("navigationBars=${navigationBars.getBottom(density)}")
        Text("navigationBarsIgnoringVisibility=${navigationBarsIgnoringVisibility.getBottom(density)}")
        HorizontalDivider()
      }
      item("areSystemBarsVisible") {
        Text("areSystemBarsVisible")
        Switch(
          checked = WindowInsets.areSystemBarsVisible,
          onCheckedChange = {
            val type = WindowInsetsCompat.Type.systemBars()
            if (it) {
              controller.show(type)
            } else {
              controller.hide(type)
            }
          },
        )
        val systemBars = WindowInsets.systemBars
        val systemBarsIgnoringVisibility = WindowInsets.systemBarsIgnoringVisibility
        Text("systemBars=${systemBars.asPureBounds(density)}")
        Text("systemBarsIgnoringVisibility=${systemBarsIgnoringVisibility.asPureBounds(density)}")

        HorizontalDivider()
      }

      item("areStatusBarsVisible") {
        Text("areStatusBarsVisible")
        Switch(
          checked = WindowInsets.areStatusBarsVisible,
          onCheckedChange = {
            val type = WindowInsetsCompat.Type.statusBars()
            if (it) {
              controller.show(type)
            } else {
              controller.hide(type)
            }
          },
        )
        val statusBars = WindowInsets.statusBars
        val statusBarsIgnoringVisibility = WindowInsets.statusBarsIgnoringVisibility

        Text("statusBars=${statusBars.getTop(density)}")
        Text("statusBarsIgnoringVisibility=${statusBarsIgnoringVisibility.getTop(density)}")
        HorizontalDivider()
      }

      item("navigationBarsBehavior") {
        Text("navigationBarsBehavior")
        val behaviors = remember {
          NavigationBarsBehavior.entries.associateBy { it.name }
        }
        SingleChoiceSegmentedButtonRow {
          behaviors.toList().forEachIndexed { index, (name, behavior) ->
            SegmentedButton(
              shape = SegmentedButtonDefaults.itemShape(index = index, count = behaviors.size),
              onClick = { navigationBarsBehavior = behavior },
              selected = navigationBarsBehavior == behavior
            ) {
              Text(name, fontSize = 9.sp)
            }
          }
        }
        HorizontalDivider()
      }
    }
  }
}

private val insetsControllerWM = WeakHashMap<Window, InsetsController>()

@Composable
fun rememberInsetsController() = findWindow()?.let { window ->
  insetsControllerWM.getOrPut(window) {
    remember(window) {
      InsetsController(window)
    }
  }
}

@Composable
fun findWindow(): Window? = LocalView.current.let { view ->
  remember(view) {
    (view.parent as? DialogWindowProvider)?.window ?: view.context.findWindow()
  }
}

private tailrec fun Context.findWindow(): Window? = when (this) {
  is Activity -> window
  is ContextWrapper -> baseContext.findWindow()
  else -> null
}