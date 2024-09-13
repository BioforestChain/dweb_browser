@file:Suppress("FunctionName", "unused")

package org.dweb_browser.shared

//import org.dweb_browser.sys.microphone.MicroPhoneNMM
import org.dweb_browser.browser.DwebBrowserLauncher
import org.dweb_browser.core.module.nativeMicroModuleUIApplication
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
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

  /// iOS版本默认启用新版桌面
  envSwitch.init(ENV_SWITCH_KEY.DESKTOP_STYLE_COMPOSE) { "true" }

  /// iOS版本Browser默认不启用原声UI
  envSwitch.disable(ENV_SWITCH_KEY.BROWSERS_NATIVE_RENDER)

  val launcher = DwebBrowserLauncher(if (debugMode) debugTags else emptyList())
  dnsNMM = launcher.dnsNMM
  val dnsRuntime = launcher.launch()
  envSwitch.watch(ENV_SWITCH_KEY.SCREEN_EDGE_SWIPE_ENABLE){
    dwebViewController.updateEdgeSwipeEnable(envSwitch.isEnabled(ENV_SWITCH_KEY.SCREEN_EDGE_SWIPE_ENABLE))
  }
  // 启动的时候就开始监听deeplink
  dwebDeepLinkHook.deeplinkSignal.listen {
    dnsRuntime.nativeFetch(it)
  }
  return dnsNMM
}

