package info.bagen.dwebbrowser.microService.browser.jmm.render

import android.webkit.WebView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.microService.browser.jmm.ui.LocalShowWebViewVersion

@Composable
internal fun DialogForWebviewVersion(appName: String) {
  val showDialog = LocalShowWebViewVersion.current
  val currentVersion = remember { mutableStateOf("") }
  val lowVersion = "90" // TODO 目前暂定该版本信息最低要求为90以上
  LaunchedEffect(Unit) {
    WebView.getCurrentWebViewPackage()?.versionName?.let { version -> // 获取当前WebView版本号
      currentVersion.value = version
      val split = version.split(".") // 103.0.5060.129
      if (split.first().toInt() < lowVersion.toInt()) {
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
        val split = stringResource(id = R.string.dialog_text_webview_upgrade).split("%s")
        val annotatedString = buildAnnotatedString {
          append(split[0])
          withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
            append(appName)
          }
          append(split[1])
          withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
            append("$lowVersion.0.0.0")
          }
          append(split[2])
          withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
            append(currentVersion.value)
          }
        }
        Text(text = annotatedString)
      },
      confirmButton = {
        Button(onClick = { }) {
          Text(text = stringResource(id = R.string.dialog_confirm_webview_upgrade))
        }
      },
      dismissButton = {
        Button(onClick = { showDialog.value = false }) {
          Text(text = stringResource(id = R.string.dialog_dismiss_webview_upgrade))
        }
      }
    )
  }
}

