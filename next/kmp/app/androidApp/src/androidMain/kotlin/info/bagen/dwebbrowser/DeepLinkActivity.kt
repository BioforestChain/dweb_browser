package info.bagen.dwebbrowser

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dweb_browser.browser.common.loading.LoadingView
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.platform.PureViewController

fun String.regexDeepLink() = Regex("dweb:.+").matchEntire(this)?.groupValues?.get(0)

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
      Box(contentAlignment = Alignment.Center) {
        val loading = remember { mutableStateOf(true) }
        LoadingView(show = loading)
      }
    }
  }
}
