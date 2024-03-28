package info.bagen.dwebbrowser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch
import org.dweb_browser.browser.common.loading.LoadingView
import org.dweb_browser.browser.util.regexDeepLink
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.platform.PureViewController

class DeepLinkActivity : PureViewController() {
  init {
    onCreate {
      application.onCreate()
      WindowCompat.setDecorFitsSystemWindows(window, false)
      lifecycleScope.launch {
        intent.dataString?.let { uri ->
          uri.regexDeepLink()?.let { dwebUri ->
            val dnsNMM = DwebBrowserApp.startMicroModuleProcess().waitPromise()
            dnsNMM.nativeFetch(dwebUri)
          }
        }
        finish()
      }
    }

    addContent {
      val showBackground = intent.getBooleanExtra("showBackground", false)
      Box(
        contentAlignment = Alignment.Center,
        modifier = if (showBackground) Modifier.fillMaxSize().background(
          MaterialTheme.colorScheme.background
        ) else Modifier.fillMaxSize()
      ) {
        val loading = remember { mutableStateOf(true) }
        LoadingView(show = loading)
      }
    }
  }
}
