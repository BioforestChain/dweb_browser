package info.bagen.dwebbrowser.microService.browser.desktop

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.web.WebView
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.base.BaseActivity
import info.bagen.dwebbrowser.microService.browser.desktop.model.LocalDesktopViewItem
import info.bagen.dwebbrowser.microService.browser.desktop.model.LocalInstallList
import info.bagen.dwebbrowser.microService.browser.desktop.model.LocalOpenList
import info.bagen.dwebbrowser.microService.browser.desktop.ui.DesktopMainView
import info.bagen.dwebbrowser.ui.theme.DwebBrowserAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.base.DWebViewItem

class DesktopActivity : BaseActivity() {

  private var controller: DesktopController? = null
  private fun upsetRemoteMmid() {
    controller?.activity = null

    controller = DesktopNMM.desktopController?.also { desktopController ->
      desktopController.activity = this
    } ?: throw Exception("no found controller")
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    upsetRemoteMmid()
    controller?.let { desktopController ->
      val context = this@DesktopActivity
      context.startActivity(Intent(context, TaskbarActivity::class.java).also {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      });

      setContent {
        DwebBrowserAppTheme {
          val scope = rememberCoroutineScope()
          desktopController.effect(activity = this@DesktopActivity)

          CompositionLocalProvider(
            LocalInstallList provides desktopController.getInstallApps(),
            LocalOpenList provides desktopController.getOpenApps(),
            LocalDesktopViewItem provides desktopController.createMainDwebView(),
          ) {
            DesktopMainView(
              viewItem = LocalDesktopViewItem.current
            ) { metaData ->
              scope.launch {
                desktopController.openApp(metaData)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun DesktopMainWebView(viewItem: DWebViewItem) {
  WebView(
    state = viewItem.state, modifier = Modifier.fillMaxSize()
  ) {
    viewItem.webView
  }
}