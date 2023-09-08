package info.bagen.dwebbrowser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import org.dweb_browser.helper.compose.AutoResizeTextContainer
import org.dweb_browser.helper.compose.AutoSizeText
import org.dweb_browser.helper.platform.OffscreenWebCanvas
import org.dweb_browser.shared.ImageLoaderDemo
import platform.UIKit.UIViewController

@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController = ComposeUIViewController {
  Column {
    Box(modifier = Modifier.size(200.dp, 50.dp)) {
      AutoResizeTextContainer {
        AutoSizeText("你好！！")
      }
    }
    val webCanvas = remember { OffscreenWebCanvas() }
    ImageLoaderDemo(webCanvas)
  }
}

