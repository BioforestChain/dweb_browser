package info.bagen.dwebbrowser

import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch
import org.dweb_browser.browser.util.regexDeepLink
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.sys.toast.ext.showToast

class DeepLinkActivity : PureViewController() {
  init {
    onCreate {
      application.onCreate()
      WindowCompat.setDecorFitsSystemWindows(window, false)
      lifecycleScope.launch {
        intent.dataString?.let { uri ->
          uri.regexDeepLink()?.let { dwebUri ->
            val dnsNMM = DwebBrowserApp.startMicroModuleProcess().await()
            val response = dnsNMM.runtime.nativeFetch(dwebUri)
            if (!response.isOk) {
              dnsNMM.runtime.showToast("DeepLink Error -> ${response.status.value}>${response.status.description}")
            }
          }
        }
        finish()
      }
    }

//    addContent {
//      val showBackground = intent.getBooleanExtra("showBackground", false)
//      Box(
//        contentAlignment = Alignment.Center,
//        modifier = if (showBackground) Modifier.fillMaxSize().background(
//          MaterialTheme.colorScheme.background
//        ) else Modifier.fillMaxSize()
//      ) {
//        val loading = remember { mutableStateOf(true) }
//        LoadingView(show = loading)
//      }
//    }
  }
}
