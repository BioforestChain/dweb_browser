package info.bagen.dwebbrowser.microService.browser.jmm.render

import android.webkit.WebView
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.microService.browser.jmm.ui.LocalShowWebViewVersion
import org.dweb_browser.browserUI.bookmark.clickableWithNoEffect
import org.dweb_browser.browserUI.download.isGreaterThan
import org.dweb_browser.browserUI.ui.loading.LocalLoadingViewManager
import org.dweb_browser.browserUI.ui.view.LocalCommonUrl
import org.dweb_browser.browserUI.util.SupportUrl

@Preview
@Composable
internal fun DialogForWebviewVersion() {
  val showDialog = LocalShowWebViewVersion.current
  val loadingManager = LocalLoadingViewManager.current
  val loadingUrl = LocalCommonUrl.current
  val currentVersion = remember { mutableStateOf("") }
  val lowVersion = "96.0.4664.104" // TODO 目前暂定该版本信息最低要求为96.0.4664.104以上
  LaunchedEffect(Unit) {
    WebView.getCurrentWebViewPackage()?.versionName?.let { version -> // 获取当前WebView版本号
      currentVersion.value = version // 103.0.5060.129
      if (lowVersion.isGreaterThan(version)) {
        showDialog.value = true
      }
    }
  }
  if (showDialog.value) {
    AlertDialog(
      onDismissRequest = { /*showDialog.value = false*/ },
      title = {
        Text(text = stringResource(id = R.string.dialog_title_webview_upgrade))
      },
      text = {
        val text = String.format(
          stringResource(id = R.string.dialog_text_webview_upgrade),
          lowVersion
        )
        Text(text = text)
      },
      confirmButton = {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = stringResource(id = R.string.dialog_dismiss_webview_upgrade),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .weight(1f)
              .clickableWithNoEffect {
                showDialog.value = false
                loadingUrl.value = SupportUrl
                loadingManager.show.value = true
              }
          )

          Button(onClick = { showDialog.value = false }) {
            Text(text = stringResource(id = R.string.dialog_confirm_webview_upgrade))
          }
        }
      }
    )
  }
}

