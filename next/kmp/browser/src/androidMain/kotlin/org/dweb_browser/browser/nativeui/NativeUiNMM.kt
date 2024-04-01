package org.dweb_browser.browser.nativeui

import org.dweb_browser.browser.nativeui.navigationBar.NavigationBarNMM
import org.dweb_browser.browser.nativeui.safeArea.SafeAreaNMM
import org.dweb_browser.browser.nativeui.splashScreen.SplashScreenNMM
import org.dweb_browser.browser.nativeui.statusBar.StatusBarNMM
import org.dweb_browser.browser.nativeui.virtualKeyboard.VirtualKeyboardNMM
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule

class NativeUiNMM : NativeMicroModule.NativeRuntime("nativeui.browser.dweb", "nativeUi") {
  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Render_Service);
  }

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