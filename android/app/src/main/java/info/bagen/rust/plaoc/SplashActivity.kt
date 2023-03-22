package info.bagen.rust.plaoc

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebView
import com.google.accompanist.web.WebViewState
import info.bagen.rust.plaoc.microService.browser.BrowserNMM
import info.bagen.rust.plaoc.microService.startDwebBrowser
import info.bagen.rust.plaoc.ui.splash.SplashPrivacyDialog
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme
import info.bagen.rust.plaoc.util.KEY_ENABLE_AGREEMENT
import info.bagen.rust.plaoc.util.getBoolean
import info.bagen.rust.plaoc.util.saveBoolean
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

    val enable = this.getBoolean(KEY_ENABLE_AGREEMENT, false)
    setContent {
      RustApplicationTheme {
        val webUrl = remember { mutableStateOf("") }
        SplashMainView()
        if (!enable) {
          SplashPrivacyDialog(
            openHome = {
              App.appContext.saveBoolean(KEY_ENABLE_AGREEMENT, true)
              startDwebBrowserProcess()
            },
            openWebView = { url -> webUrl.value = url },
            closeApp = { finish() }
          )
          PrivacyView(url = webUrl)
        } else {
          startDwebBrowserProcess()
        }
      }
    }
  }

  override fun onStop() {
    super.onStop()
    finish()
  }
}

private fun startDwebBrowserProcess() {
  GlobalScope.launch {
    if (App.browserActivity == null) {
      startDwebBrowser()
    } else {
      delay(1000)
      BrowserNMM.browserController.openBrowserActivity()
    }
  }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun SplashMainView() {
  Column(modifier = Modifier.fillMaxSize()) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      val gradient = listOf(
        Color(0xFF71D78E), Color(0xFF548FE3)
      )
      Text(
        text = stringResource(id = R.string.app_name),
        modifier = Modifier.align(Alignment.BottomCenter),
        style = TextStyle(
          brush = Brush.linearGradient(gradient), fontSize = 50.sp
        )
      )
    }
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    )
  }
}

@Composable
fun PrivacyView(url: MutableState<String>) {
  BackHandler { url.value = "" }
  if (url.value.isNotEmpty()) {
    WebView(state = WebViewState(WebContent.Url(url.value)), modifier = Modifier.fillMaxSize())
  }
}