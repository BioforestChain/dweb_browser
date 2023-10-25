package info.bagen.dwebbrowser.microService.sys.deepLink

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import info.bagen.dwebbrowser.App
import kotlinx.coroutines.launch
import org.dweb_browser.browser.web.ui.loading.LoadingView
import org.dweb_browser.core.module.BaseThemeActivity
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.compose.theme.DwebBrowserAppTheme

class DeepLinkActivity : BaseThemeActivity() {
  companion object {
    fun String.regexDeepLink() = Regex("dweb:.+").matchEntire(this)?.groupValues?.get(0)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    application.onCreate()
    WindowCompat.setDecorFitsSystemWindows(window, false)
    lifecycleScope.launch {
      intent.dataString?.let { uri ->
        uri.regexDeepLink()?.let { dwebUri ->
          val dnsNMM = App.startMicroModuleProcess().waitPromise();
          dnsNMM.nativeFetch(dwebUri)
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
