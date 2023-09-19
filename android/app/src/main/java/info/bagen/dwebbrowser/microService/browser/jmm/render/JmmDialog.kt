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
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.microService.browser.jmm.ui.LocalShowWebViewVersion
import org.dweb_browser.browserUI.bookmark.clickableWithNoEffect
import org.dweb_browser.browserUI.download.isGreaterThan
import org.dweb_browser.browserUI.ui.view.LocalCommonUrl
import org.dweb_browser.browserUI.util.SupportUrl

@Composable
internal fun DialogForWebviewVersion() {
  val showDialog = LocalShowWebViewVersion.current
  val loadingUrl = LocalCommonUrl.current
  val currentVersion = remember { mutableStateOf("") }
  val lowVersion = "96.0.4664.104" // TODO 目前暂定该版本信息最低要求为96.0.4664.104以上
  LaunchedEffect(Unit) {
    WebView.getCurrentWebViewPackage()?.let { webViewPackage -> // 获取当前WebView版本号
      // 这边过滤华为的webview版本：com.huawei.webview,,,android.webkit.webview
      if (webViewPackage.packageName == "com.google.android.webview" &&
        lowVersion.isGreaterThan(webViewPackage.versionName)
      ) {
        currentVersion.value = webViewPackage.versionName // 103.0.5060.129
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
                loadingUrl.value = SupportUrl // 地址变化，会引起webview加载，加载状态决定是否显示loading
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

