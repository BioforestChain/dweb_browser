package org.dweb_browser.browser.jmm.render

import android.webkit.WebView
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.ui.LocalShowWebViewVersion
import org.dweb_browser.helper.SupportUrl
import org.dweb_browser.helper.compose.LocalCommonUrl
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.isGreaterThan

@Composable
internal fun WebviewVersionWarningDialog() {
  var isShowDialog by LocalShowWebViewVersion.current
  val loadingUrl = LocalCommonUrl.current
  var myVersion by remember { mutableStateOf("") }
  val lowVersion = "96.0.4664.104" // TODO 目前暂定该版本信息最低要求为96.0.4664.104以上
  LaunchedEffect(Unit) {
    WebView.getCurrentWebViewPackage()?.let { webViewPackage -> // 获取当前WebView版本号
      // 这边过滤华为的webview版本：com.huawei.webview,,,android.webkit.webview
      if (webViewPackage.packageName == "com.google.android.webview" && lowVersion.isGreaterThan(
          webViewPackage.versionName
        )
      ) {
        myVersion = webViewPackage.versionName // 103.0.5060.129
        isShowDialog = true
      }
    }
  }
  if (isShowDialog) {
    AlertDialog(onDismissRequest = { /*showDialog = false*/ }, title = {
      Text(text = BrowserI18nResource.dialog_title_webview_upgrade())
    }, text = {
      Text(text = BrowserI18nResource.dialog_text_webview_upgrade {
        requiredVersion = lowVersion
        currentVersion = myVersion
      })
    }, confirmButton = {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = BrowserI18nResource.dialog_dismiss_webview_upgrade(),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier
            .weight(1f)
            .clickableWithNoEffect {
              isShowDialog = false
              loadingUrl.value = SupportUrl // 地址变化，会引起webview加载，加载状态决定是否显示loading
            })

        Button(onClick = { isShowDialog = false }) {
          Text(text = BrowserI18nResource.dialog_confirm_webview_upgrade())
        }
      }
    })
  }
}

