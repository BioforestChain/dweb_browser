package info.bagen.dwebbrowser.microService.sys.deepLink

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.lifecycle.lifecycleScope
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.base.BaseThemeActivity
import kotlinx.coroutines.launch
import org.dweb_browser.browserUI.ui.loading.LoadingView
import org.dweb_browser.helper.compose.theme.DwebBrowserAppTheme
import org.dweb_browser.microservice.sys.dns.nativeFetch

class DeepLinkActivity : BaseThemeActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    println("DeepLink onCreate enter")

    application.onCreate()

    lifecycleScope.launch {
      intent.dataString?.let { uri ->
        Regex("dweb:.+").matchEntire(uri)?.groupValues?.get(0)?.let { dweb_uri ->
          App.startMicroModuleProcess().nativeFetch(dweb_uri)
        }
      }
      finish()
    }

    setContent {
      DwebBrowserAppTheme {
        Box(contentAlignment = Alignment.Center) {
          val loading = remember { mutableStateOf(true) }
          LoadingView(show = loading)
        }
      }
    }
  }
}