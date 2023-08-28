package info.bagen.dwebbrowser.microService.browser.nativeui

import info.bagen.dwebbrowser.microService.browser.nativeui.navigationBar.NavigationBarNMM
import info.bagen.dwebbrowser.microService.browser.nativeui.safeArea.SafeAreaNMM
import info.bagen.dwebbrowser.microService.browser.nativeui.splashScreen.SplashScreenNMM
import info.bagen.dwebbrowser.microService.browser.nativeui.statusBar.StatusBarNMM
import info.bagen.dwebbrowser.microService.browser.nativeui.virtualKeyboard.VirtualKeyboardNMM
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY

class NativeUiNMM : NativeMicroModule("nativeui.browser.dweb", "nativeUi") {

  override val categories =
    mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Render_Service);

  private val navigationBarNMM = NavigationBarNMM()
  private val statusBarNMM = StatusBarNMM()
  private val safeAreaNMM = SafeAreaNMM()
  private val virtualKeyboardNMM = VirtualKeyboardNMM()
  private val splashScreenNMM = SplashScreenNMM()

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
//        bootstrapContext.dns.install(navigationBarNMM)
//        bootstrapContext.dns.install(statusBarNMM)
//        bootstrapContext.dns.install(safeAreaNMM)
//        bootstrapContext.dns.install(virtualKeyboardNMM)
//        bootstrapContext.dns.install(splashScreenNMM)
  }

  override suspend fun _shutdown() {
  }
}