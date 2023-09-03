package org.dweb_browser.app.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.dweb_browser.helper.compose.AutoResizeTextContainer
import org.dweb_browser.helper.compose.AutoSizeText
import org.dweb_browser.helper.compose.SimpleBox
import org.dweb_browser.shared.Greeting

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colors.background
        ) {
          GreetingView(Greeting().greet())
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
