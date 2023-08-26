package org.dweb_browser.window.render

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.twotone.DarkMode
import androidx.compose.material.icons.twotone.LightMode
import androidx.compose.material.icons.twotone.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.RichTooltipBox
import androidx.compose.material3.RichTooltipColors
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberRichTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.window.core.WindowController
import org.dweb_browser.window.core.constant.WindowColorScheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WindowMenuPanel(
  win: WindowController,
) {
  val winPadding = LocalWindowPadding.current
  val winTheme = LocalWindowControllerTheme.current
  val isShowMenuPanel by win.watchedState { showMenuPanel }
  val tooltipState = rememberRichTooltipState(isPersistent = true)
  val scope = rememberCoroutineScope()

  LaunchedEffect(tooltipState.isVisible) {
    scope.launch {
      if (!tooltipState.isVisible) {
        win.hideMenuPanel()
      }
    }
  }
  LaunchedEffect(isShowMenuPanel) {
    scope.launch {
      if (isShowMenuPanel) {
        tooltipState.show()
      } else {
        tooltipState.dismiss()
      }
    }
  }
  val shape = remember(winPadding.boxRounded) {
    winPadding.boxRounded.toRoundedCornerShape()
  }
  val colors = remember(winTheme) {
    RichTooltipColors(
      containerColor = winTheme.themeColor,
      contentColor = winTheme.themeContentColor,
      titleContentColor = winTheme.themeContentColor,
      actionContentColor = winTheme.themeContentColor,
    )
  }
  RichTooltipBox(
    tooltipState = tooltipState,
    shape = shape,
    colors = colors,
    title = {
      Row {
        val owner by win.watchedState { owner }
        win.IconRender(modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = owner)
      }
    },
    text = {
      val winMenuItemColor = remember(winTheme) {
        NavigationRailItemColors(
          unselectedIconColor = winTheme.onThemeContentDisableColor,
          selectedIconColor = winTheme.themeContentColor,
          selectedIndicatorColor = winTheme.onThemeContentColor,

          unselectedTextColor = winTheme.onThemeContentColor,
          selectedTextColor = winTheme.onThemeContentColor,

          disabledIconColor = winTheme.themeContentDisableColor,
          disabledTextColor = winTheme.themeContentDisableColor,
        )
      }

      @Composable
      fun WindowMenuItem(
        iconVector: ImageVector,
        labelText: String,
        selected: Boolean,
        selectedIconVector: ImageVector = iconVector,
        onClick: suspend () -> Unit
      ) {
        NavigationRailItem(
          colors = winMenuItemColor,
          icon = {
            Icon(
              if (selected) selectedIconVector else iconVector,
              contentDescription = labelText,
            )
          },
          label = {
            Text(
              labelText,
              fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f,
              textAlign = TextAlign.Center
            )
          },
          selected = selected,
          onClick = {
            scope.launch { onClick() }
          },
        )
      }
      Box(
        modifier = Modifier.padding(top = 12.dp)
      ) {
        LazyVerticalGrid(
          columns = GridCells.Adaptive(68.dp),
          modifier = Modifier
            .clip(
              winPadding.boxRounded.toRoundedCornerShape()
            )
            .background(winTheme.onThemeColor),
          contentPadding = PaddingValues(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
        ) {
          /// 窗口置顶
          item {
            val isAlwaysOnTop by win.watchedState { alwaysOnTop }
            WindowMenuItem(
              iconVector = Icons.Outlined.PushPin,
              selectedIconVector = Icons.TwoTone.PushPin,
              labelText = "置顶",
              selected = isAlwaysOnTop,
            ) { win.toggleAlwaysOnTop() }
          }
          /// 窗口颜色偏好
          item {
            val colorScheme by win.watchedState { colorScheme }
            val isCustomColorScheme = colorScheme != WindowColorScheme.Normal
            val isSystemInDark = isSystemInDarkTheme()
            WindowMenuItem(
              iconVector = when (colorScheme) {
                WindowColorScheme.Normal -> if (isSystemInDark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode
                WindowColorScheme.Light -> Icons.Outlined.LightMode
                WindowColorScheme.Dark -> Icons.Outlined.DarkMode
              },
              selectedIconVector = when (colorScheme) {
                WindowColorScheme.Normal -> if (isSystemInDark) Icons.TwoTone.DarkMode else Icons.TwoTone.LightMode
                WindowColorScheme.Light -> Icons.TwoTone.LightMode
                WindowColorScheme.Dark -> Icons.TwoTone.DarkMode
              },
              labelText = when (colorScheme) {
                WindowColorScheme.Normal -> "默认"
                WindowColorScheme.Light -> "亮色"
                WindowColorScheme.Dark -> "深色"
              },
              selected = isCustomColorScheme,
            ) { win.toggleColorScheme() }
          }
          // 分享应用 ！
          // 截图 ！
          // 窗口快速布局（resize到合理的大小） ！
          // 自定义shortcut（扫一扫）
          // 自定义widget
          // 卸载 ！
          // 更新！
          // 定位权限
          // 网络权限
          // 蓝牙权限
          // 用户空间
          // 存储管理
          // 反馈开发者（一个简单的系统。邮箱？issues-template）
          // 使用说明
          // 更新日志
          // 授权协议
          // 官网！
          // 开发者信息
          // 语言偏好
          // 签名验证是否通过？
          // 实时日志信息
        }
      }
    },
    action = {
      TextButton(onClick = { scope.launch { win.hideMenuPanel() } }) { Text("退出应用") }
    },
  ) {}
}

