package info.bagen.dwebbrowser

import androidx.compose.ui.window.ComposeUIViewController
import org.dweb_browser.helper.compose.AutoResizeTextContainer
import org.dweb_browser.helper.compose.AutoSizeText
import platform.UIKit.UIViewController

@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController =
  ComposeUIViewController {
    AutoResizeTextContainer {
      AutoSizeText("你好！！")
    }
  }

