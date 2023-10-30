package info.bagen.dwebbrowser

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.app.android.MyApplicationTheme
import org.dweb_browser.helper.compose.AutoResizeTextContainer
import org.dweb_browser.helper.compose.AutoSizeText
import org.dweb_browser.helper.platform.JsRuntime
import org.dweb_browser.shared.Greeting
import org.dweb_browser.shared.ImageLoaderDemo
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.WindowPreviewer
import org.dweb_browser.sys.window.render.watchedState

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    val jsRuntime = JsRuntime(this);
    super.onCreate(savedInstanceState)
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
        ) {
          Column(Modifier.verticalScroll(rememberScrollState())) {
            val scope = rememberCoroutineScope()
            Row {
              Button({
                scope.launch {
                  jsRuntime.core.testNative2Js()
                }
              }) {
                Text("Native2Js")
              }
              Button({
                scope.launch {
                  jsRuntime.core.testJs2Native()
                }
              }) {
                Text("Js2Native")
              }
            }
            Row {
              Button({
                scope.launch {
                  jsRuntime.core.testNative2Js2()
                }
              }) {
                Text("Native2Js2")
              }
              Button({
                scope.launch {
                  jsRuntime.core.testJs2Native2()
                }
              }) {
                Text("Js2Native2")
              }
            }
            Row {
              Button({
                scope.launch {
                  jsRuntime.core.testNative2Js4()
                }
              }) {
                Text("Native2Js4")
              }
              Button({
                scope.launch {
                  jsRuntime.core.testJs2Native4()
                }
              }) {
                Text("Js2Native4")
              }
            }
            GreetingView(Greeting().greet())
            PreviewWindowTopBar()
          }
        }
      }
    }
  }
}


@SuppressLint("ProduceStateDoesNotAssignValue")
@Composable
fun GreetingView(text: String) {
  Column {
    Box(Modifier.height(50.dp)) {
      AutoResizeTextContainer {
        AutoSizeText(text = text)
      }
    }
    Text("qaq")
    Box(Modifier.height(400.dp).background(Color.LightGray)) {
      ImageLoaderDemo()
    }
  }
}


//@Preview
//@Composable
//fun DefaultPreview() {
//  MyApplicationTheme {
//    GreetingView("Hello, Android!")
//  }
//}
//

@Composable
fun PreviewWindowTopBarContent(modifier: Modifier) {
  Box(
    modifier.background(Color.DarkGray)
  ) {
    val iconUrl by LocalWindowController.current.watchedState { iconUrl ?: "" }
    TextField(iconUrl, onValueChange = {}, modifier = Modifier.fillMaxSize())
  }
}

@Composable
fun PreviewWindowTopBar() {
  WindowPreviewer(modifier = Modifier.height(500.dp), config = {
    state.title = "应用长长的标题的标题的标题～～"
    state.topBarContentColor = "#FF00FF"
    state.themeColor = "#Fd9F9F"
    state.iconUrl = "http://172.30.92.50:12207/m3-favicon-apple-touch.png"
    state.iconMaskable = true
    state.showMenuPanel = true
  }) { modifier ->
    PreviewWindowTopBarContent(modifier)
  }
}