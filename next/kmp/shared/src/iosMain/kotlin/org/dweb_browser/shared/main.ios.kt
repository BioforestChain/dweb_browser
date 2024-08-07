@file:Suppress("FunctionName", "unused")

package org.dweb_browser.shared

//import org.dweb_browser.sys.microphone.MicroPhoneNMM
import org.dweb_browser.browser.DwebBrowserLauncher
import org.dweb_browser.core.module.nativeMicroModuleUIApplication
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.platform.DeepLinkHook.Companion.deepLinkHook
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.pure.http.PureResponse
import platform.UIKit.UIApplication

val dwebViewController = nativeViewController
val dwebDeepLinkHook = deepLinkHook
private lateinit var dnsNMM: DnsNMM

suspend fun dnsFetch(url: String): PureResponse {
  return dnsNMM.runtime.nativeFetch(url)
}

suspend fun startDwebBrowser(
  app: UIApplication,
  debugMode: Boolean,
  debugTags: List<String> = listOf("/.+/"),
): DnsNMM {
  nativeMicroModuleUIApplication = app;

  val launcher = DwebBrowserLauncher(if (debugMode) debugTags else emptyList())
  dnsNMM = launcher.dnsNMM
  val dnsRuntime = launcher.launch()

  // 启动的时候就开始监听deeplink
  dwebDeepLinkHook.deeplinkSignal.listen {
    dnsRuntime.nativeFetch(it)
  }
  return dnsNMM
}

