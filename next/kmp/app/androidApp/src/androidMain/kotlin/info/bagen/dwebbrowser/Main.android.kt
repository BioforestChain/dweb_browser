package info.bagen.dwebbrowser

import android.webkit.WebView
import org.dweb_browser.browser.DwebBrowserLauncher
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch

suspend fun startApplication(): DnsNMM {
  /// 启动Web调试
  WebView.setWebContentsDebuggingEnabled(true)

  /// Android版本默认启用新版桌面
  envSwitch.init(ENV_SWITCH_KEY.DESKTOP_STYLE_COMPOSE) { "true" }

  val launcher = DwebBrowserLauncher(if (BuildConfig.DEBUG) "/.+/" else null)
  launcher.launch()
  return launcher.dnsNMM
}
