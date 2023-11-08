package info.bagen.dwebbrowser.microService.sys

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Light
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.dweb_browser.core.help.types.MMID

enum class PluginType(
  val mmid: MMID, private val typeName: String, val description: String, val icon: ImageVector
) {
  BarcodeScanning(
    "barcode-scanning.sys.dweb",
    "扫码插件",
    "打开相机，扫描二维码并获取二维码内的数据",
    Icons.Default.Scanner
  ),
  Biometrics("biometrics.sys.dweb", "生物识别插件", "指纹识别插件", Icons.Default.Fingerprint),
  Camera("camera.sys.dweb", "相机插件", "用于应用拍摄照片和视频", Icons.Default.Camera),
  DwebServiceWorker(
    "jmm.browser.dweb",
    "应用管理插件",
    "下载、更新相关应用操作",
    Icons.Default.Download
  ),
  FileSystem("file.std.dweb", "文件管理插件", "管理系统文件", Icons.Default.FileOpen),
  Haptics("haptics.sys.dweb", "振动相关功能插件", "用于手机静音等", Icons.Default.RingVolume),
  NavigationBar("dweb-navigation-bar", "导航栏插件", "获取导航栏的宽高", Icons.Default.Navigation),
  StatusBar(
    "status-bar.nativeui.browser.dweb",
    "状态栏插件",
    "获取状态栏的宽高",
    Icons.Default.Settings
  ),
  SafeArea("safe-area.nativeui.browser.dweb", "安全区插件", "获取安全区大小", Icons.Default.Camera),
  Torch("torch.nativeui.browser.dweb", "手电筒插件", "打开和关闭手电筒功能", Icons.Default.Light),
  VirtualKeyboard(
    "virtual-keyboard.nativeui.browser.dweb",
    "虚拟键盘插件",
    "控制键盘的显示和隐藏",
    Icons.Default.Keyboard
  ),
  Share("share.sys.dweb", "分享插件", "信息分享功能", Icons.Default.Share),
  Toast("toast.sys.dweb", "通知插件", "消息提示功能", Icons.Default.Message),
  ;

  @Composable
  fun PluginsView() {
    ListItem(
      leadingContent = { Icon(this.icon, contentDescription = this.name) },
      headlineContent = { Text(text = this.typeName) },
      supportingContent = { Text(text = this.description) }
    )
  }
}