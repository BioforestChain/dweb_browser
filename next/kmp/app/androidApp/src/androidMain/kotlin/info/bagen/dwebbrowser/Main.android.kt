package info.bagen.dwebbrowser

import android.webkit.WebView
import org.dweb_browser.browser.DwebBrowserLauncher
import org.dweb_browser.core.std.dns.DnsNMM

suspend fun startDwebBrowser(): DnsNMM {
  /// 启动Web调试
  WebView.setWebContentsDebuggingEnabled(true)

  val launcher = DwebBrowserLauncher(if (BuildConfig.DEBUG) "/.+/" else null)
  launcher.launch()
  return launcher.dnsNMM
}
