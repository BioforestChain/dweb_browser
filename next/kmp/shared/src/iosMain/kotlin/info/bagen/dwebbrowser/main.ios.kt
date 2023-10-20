package info.bagen.dwebbrowser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.AutoResizeTextContainer
import org.dweb_browser.helper.compose.AutoSizeText
import org.dweb_browser.helper.platform.JsRuntime
import org.dweb_browser.shared.ImageLoaderDemo
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.WindowPreviewer
import org.dweb_browser.sys.window.render.watchedState
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIColor.Companion.blueColor
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.WebKit.WKWebView
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURL

@Suppress("FunctionName", "unused")
fun MainViewController(iosView: UIView): UIViewController = ComposeUIViewController {
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
//    Box(modifier = Modifier.size(200.dp, 50.dp)) {
//      AutoResizeTextContainer {
//        AutoSizeText("你好！！")
//      }
//    }
//    val jsRuntime = JsRuntime();
//    val scope = rememberCoroutineScope()
//    Row {
//      Button({
//        scope.launch {
//          jsRuntime.core.testNative2Js()
//        }
//      }) {
//        Text("Native2Js")
//      }
//      Button({
//        scope.launch {
//          jsRuntime.core.testJs2Native()
//        }
//      }) {
//        Text("Js2Native")
//      }
//    }
//    Row {
//      Button({
//        scope.launch {
//          jsRuntime.core.testNative2Js2()
//        }
//      }) {
//        Text("Native2Js2")
//      }
//      Button({
//        scope.launch {
//          jsRuntime.core.testJs2Native2()
//        }
//      }) {
//        Text("Js2Native2")
//      }
//    }
//    Row {
//      Button({
//        scope.launch {
//          jsRuntime.core.testNative2Js4()
//        }
//      }) {
//        Text("Native2Js4")
//      }
//      Button({
//        scope.launch {
//          jsRuntime.core.testJs2Native4()
//        }
//      }) {
//        Text("Js2Native4")
//      }
//    }
//    Box(Modifier.height(400.dp).background(Color.LightGray)) {
//      ImageLoaderDemo()
//    }
    PreviewWindowTopBar(iosView)
  }
}


@OptIn(ExperimentalForeignApi::class)
@Composable
fun PreviewWindowTopBar(iosView: UIView) {
  WindowPreviewer(modifier = Modifier.height(500.dp), config = {
    state.title = "应用长长的标题的标题的标题～～"
    state.topBarContentColor = "#FF00FF"
    state.themeColor = "#Fd9F9F"
    state.iconUrl = "http://172.30.92.50:12207/m3-favicon-apple-touch.png"
    state.iconMaskable = true
    state.showMenuPanel = true
  }) { modifier ->

    UIKitView(factory =
    {
      iosView
//        WKWebView().also {
//            it.loadRequest(NSURLRequest.requestWithURL(NSURL.URLWithString("https://m.163.com")?:throw Exception()))
//        }

    },modifier
    )
//    PreviewWindowTopBarContent(modifier)
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