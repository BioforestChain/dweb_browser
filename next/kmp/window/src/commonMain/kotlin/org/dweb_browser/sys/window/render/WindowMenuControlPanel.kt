package org.dweb_browser.sys.window.render

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.HelpCenter
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.twotone.Assignment
import androidx.compose.material.icons.automirrored.twotone.HelpCenter
import androidx.compose.material.icons.automirrored.twotone.VolumeUp
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LayersClear
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WifiTetheringOff
import androidx.compose.material.icons.twotone.Bluetooth
import androidx.compose.material.icons.twotone.DarkMode
import androidx.compose.material.icons.twotone.DataUsage
import androidx.compose.material.icons.twotone.DeveloperBoard
import androidx.compose.material.icons.twotone.DeveloperMode
import androidx.compose.material.icons.twotone.Feedback
import androidx.compose.material.icons.twotone.Gavel
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material.icons.twotone.Language
import androidx.compose.material.icons.twotone.Layers
import androidx.compose.material.icons.twotone.LightMode
import androidx.compose.material.icons.twotone.LocationOn
import androidx.compose.material.icons.twotone.Person
import androidx.compose.material.icons.twotone.PushPin
import androidx.compose.material.icons.twotone.VerifiedUser
import androidx.compose.material.icons.twotone.WifiTethering
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.constant.WindowColorScheme
import org.dweb_browser.sys.window.helper.LocalWindowControllerTheme
import org.dweb_browser.sys.window.helper.LocalWindowFrameStyle
import org.dweb_browser.sys.window.helper.watchedBounds
import org.dweb_browser.sys.window.helper.watchedIsMaximized
import org.dweb_browser.sys.window.helper.watchedState

@Composable
fun WindowControlPanel(win: WindowController, modifier: Modifier = Modifier) {
  val winFrameStyle = LocalWindowFrameStyle.current
  val winTheme = LocalWindowControllerTheme.current
  val isMaximized by win.watchedIsMaximized()
  val winBounds by win.watchedBounds()
  val maxHeight = winBounds.height.dp
  LocalCompositionChain.current.Provider(
    LocalWindowMenuItemColor provides winTheme.toWindowMenuItemColor()
  ) {
    LazyVerticalGrid(
      columns = GridCells.Adaptive(68.dp),
      modifier = modifier.clip(
        winFrameStyle.frameRounded.roundedCornerShape
      ).background(winTheme.onThemeColor)
//      .verticalScroll(rememberScrollState()),
        .then(if (isMaximized) Modifier else Modifier.height(maxHeight - 120.dp)), // 由于窗口模式时，LazyVerticalGrid适配整个窗口，导致action显示不全。
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
        ) { win.toggleAlwaysOnTop(it) }
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
            WindowColorScheme.Normal -> "默认配色"
            WindowColorScheme.Light -> "亮色"
            WindowColorScheme.Dark -> "深色"
          },
          selected = isCustomColorScheme,
        ) { win.toggleColorScheme() }
      }
      item {
        val keepBackground by win.watchedState { keepBackground }
        WindowMenuItem(
          iconVector = Icons.Outlined.LayersClear,
          selectedIconVector = Icons.TwoTone.Layers,
          labelText = when {
            keepBackground -> "允许后台"
            else -> "前台模式"
          },
          selected = keepBackground,
        ) {
          win.toggleKeepBackground(it)
        }
      }
      @Suppress("ConstantConditionIf")
      if (true) return@LazyVerticalGrid // 下面功能暂时未完善，上架时有要求，暂时隐藏不具备的功能
      // 分享应用 ！
      item {
        WindowMenuItem(
          iconVector = Icons.Outlined.Share,
          labelText = "分享应用",
          enabled = false,
        )
      }
      // 截图 ！
      item {
        WindowMenuItem(
          iconVector = Icons.Outlined.Screenshot,
          labelText = "截图",
          enabled = false,
        )
      }
      // 窗口快速布局（resize到合理的大小） ！
      item {
        WindowMenuItem(
          iconVector = Icons.Outlined.FitScreen,
          labelText = "适配屏幕大小",
          enabled = false,
        )
      }
      // 自定义shortcut（扫一扫）
      // 自定义widget
      // 卸载 ！
      item {
        WindowMenuItem(
          iconVector = Icons.Outlined.DeleteForever,
          labelText = "卸载",
          enabled = false,
        )
      }
      // 更新！
      item {
        WindowMenuItem(
          iconVector = Icons.Outlined.FindReplace,
          labelText = "检查更新",
          enabled = false,
        )
      }
      // 定位权限
      item {
        WindowMenuItem(
          iconVector = Icons.Outlined.LocationOff,
          selectedIconVector = Icons.TwoTone.LocationOn,
          labelText = "定位权限",
          selected = true,
          enabled = false,
        )
      }
      // 网络权限
      item {
        WindowMenuItem(
          iconVector = Icons.Outlined.WifiTetheringOff,
          selectedIconVector = Icons.TwoTone.WifiTethering,
          labelText = "网络权限",
          selected = true,
          enabled = false,
        )
      }
      // 蓝牙权限
      item {
        WindowMenuItem(
          iconVector = Icons.Outlined.BluetoothDisabled,
          selectedIconVector = Icons.TwoTone.Bluetooth,
          labelText = "蓝牙权限",
          selected = true,
          enabled = false,
        )
      }
      // 音量控制
      item {
        WindowMenuItem(
          iconVector = Icons.AutoMirrored.Outlined.VolumeDown,
          selectedIconVector = Icons.AutoMirrored.TwoTone.VolumeUp,
          labelText = "音量",
          selected = true,
          enabled = false,
        )
      }
      // 用户空间
      item {
        WindowMenuItem(
          iconVector = Icons.Outlined.Person,
          selectedIconVector = Icons.TwoTone.Person,
          labelText = "账号",
          enabled = false,
        )
      }
      // 存储管理
      item {
        WindowMenuItem(
          iconVector = Icons.Outlined.DataUsage,
          selectedIconVector = Icons.TwoTone.DataUsage,
          labelText = "存储占用",
          enabled = false,
        )
      }
      // 反馈开发者（一个简单的系统。邮箱？issues-template）
      item {
        WindowMenuItem(
          iconVector = Icons.Outlined.Feedback,
          selectedIconVector = Icons.TwoTone.Feedback,
          labelText = "问题反馈",
          enabled = false,
        )
      }
      // 使用说明
      item {
        WindowMenuItem(
          iconVector = Icons.AutoMirrored.Outlined.HelpCenter,
          selectedIconVector = Icons.AutoMirrored.TwoTone.HelpCenter,
          labelText = "使用说明",
          enabled = false,
        )
      }
      // 更新日志
      item {
        WindowMenuItem(
          iconVector = Icons.AutoMirrored.Outlined.Assignment,
          selectedIconVector = Icons.AutoMirrored.TwoTone.Assignment,
          labelText = "更新日志",
          enabled = false,
        )
      }
      // 授权协议
      item {
        WindowMenuItem(
          iconVector = Icons.Outlined.Gavel,
          selectedIconVector = Icons.TwoTone.Gavel,
          labelText = "许可证",
          enabled = false,
        )
      }
      // 官网！
      item {
        WindowMenuItem(
          iconVector = Icons.Outlined.Home,
          selectedIconVector = Icons.TwoTone.Home,
          labelText = "官方主页",
          enabled = false,
        )
      }
      // 开发者信息
      item {
        WindowMenuItem(
          iconVector = Icons.Outlined.DeveloperBoard,
          selectedIconVector = Icons.TwoTone.DeveloperBoard,
          labelText = "开发者",
          enabled = false,
        )
      }
      // 语言偏好
      item {
        WindowMenuItem(
          iconVector = Icons.Outlined.Language,
          selectedIconVector = Icons.TwoTone.Language,
          labelText = "语言",
          enabled = false,
        )
      }
      // 证书列表，可以是自己颁发的证书，或者是用户自己信任的证书厂商。签名验证是否通过？
      item {
        WindowMenuItem(
          iconVector = Icons.Outlined.VerifiedUser,
          selectedIconVector = Icons.TwoTone.VerifiedUser,
          labelText = "证书",
          enabled = false,
        )
      }
      // 实时日志信息
      item {
        WindowMenuItem(
          iconVector = Icons.Outlined.DeveloperMode,
          selectedIconVector = Icons.TwoTone.DeveloperMode,
          labelText = "日志",
          enabled = false,
        )
      }
    }
  }
}