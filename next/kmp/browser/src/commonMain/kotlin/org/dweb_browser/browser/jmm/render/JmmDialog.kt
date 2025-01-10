package org.dweb_browser.browser.jmm.render

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
import io.ktor.http.encodeURLParameter
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.LocalShowWebViewVersion
import org.dweb_browser.browser.jmm.getChromeWebViewVersion
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.SupportUrl
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.isGreaterThan
import org.dweb_browser.sys.window.core.constant.LocalWindowMM

var showedWarningDialog = false

@Composable
internal fun WebviewVersionWarningDialog() {
  if (showedWarningDialog) return
  var isShowDialog by LocalShowWebViewVersion.current
  var myVersion by remember { mutableStateOf("") }
  val lowVersion = "96.0.4664.104" // TODO 目前暂定该版本信息最低要求为96.0.4664.104以上
  LaunchedEffect(Unit) {
    val version = getChromeWebViewVersion() ?: return@LaunchedEffect
    if (lowVersion.isGreaterThan(version)) {
      myVersion = version
      isShowDialog = true
    }
  }
  if (isShowDialog) {
    val microModule = LocalWindowMM.current
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
              microModule.scopeLaunch(cancelable = true) {
                microModule.nativeFetch("dweb://openinbrowser?url=${SupportUrl.encodeURLParameter()}")
              }
            })

        Button(onClick = { isShowDialog = false }) {
          Text(text = BrowserI18nResource.dialog_confirm_webview_upgrade())
        }
      }
    })
  }
}

