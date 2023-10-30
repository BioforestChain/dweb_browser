package info.bagen.dwebbrowser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.WindowPreviewer
import org.dweb_browser.sys.window.render.watchedState
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.WebKit.WKWebView
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURL
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.CoreGraphics.CGFloat
import kotlinx.cinterop.useContents


@Suppress("FunctionName", "unused")
fun MainViewController(iosView: UIView, onSizeChange: (CGFloat, CGFloat) -> Unit): UIViewController = ComposeUIViewController {
  Box(Modifier.fillMaxSize(). background(Color.Cyan)) {
    PreviewWindowTopBar(iosView, onSizeChange)
  }
}


@OptIn(ExperimentalForeignApi::class)
@Composable
fun PreviewWindowTopBar(iosView: UIView, onSizeChange: (CGFloat, CGFloat)->Unit) {
  WindowPreviewer(modifier = Modifier.width(350.dp).height(500.dp), config = {
    state.title = "应用长长的标题的标题的标题～～"
    state.topBarContentColor = "#FF00FF"
    state.themeColor = "#Fd9F9F"
    state.iconUrl = "http://172.30.92.50:12207/m3-favicon-apple-touch.png"
    state.iconMaskable = true
    state.showMenuPanel = true
  }) { modifier ->

    onSizeChange(width.toDouble(), height.toDouble())

    UIKitView(
      factory = {
        iosView
      },
      modifier =Modifier,
      update = { view ->
      println( "update:::: $view")
    })//    PreviewWindowTopBarContent(modifier)
  }
}

@Composable
fun PreviewWindowTopBarContent(modifier: Modifier) {
  Box(
    modifier.background(Color.DarkGray)
  ) {
    val iconUrl by LocalWindowController.current.watchedState { iconUrl ?: "" }
    TextField(iconUrl, onValueChange = {}, modifier = Modifier.fillMaxSize())
  }
}
