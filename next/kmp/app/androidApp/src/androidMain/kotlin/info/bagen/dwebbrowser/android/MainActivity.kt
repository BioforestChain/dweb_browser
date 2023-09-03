package org.dweb_browser.app.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.dweb_browser.shared.Greeting
import org.dweb_browser.window.render.LocalWindowController
import org.dweb_browser.window.render.WindowPreviewer
import org.dweb_browser.window.render.watchedState

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colors.background
        ) {
          Column {
            GreetingView(Greeting().greet())
            PreviewWindowTopBar()
          }
        }
      }
    }
  }
}

@Composable
fun GreetingView(text: String) {
//  AutoResizeTextContainer {
//    AutoSizeText(text = text)
//  }
//  SimpleBox()
  Text("qaq")
}

@Preview
@Composable
fun DefaultPreview() {
  MyApplicationTheme {
    GreetingView("Hello, Android!")
  }
}


@Composable
fun PreviewWindowTopBarContent(modifier: Modifier) {
  Box(
    modifier
      .background(Color.DarkGray)
  ) {
    val iconUrl by LocalWindowController.current.watchedState { iconUrl ?: "" }
    TextField(iconUrl, onValueChange = {}, modifier = Modifier.fillMaxSize())
  }
}

@Composable
fun PreviewWindowTopBar() {
  WindowPreviewer(config = {
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