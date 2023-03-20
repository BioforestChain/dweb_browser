package info.bagen.rust.plaoc.microService.sys.nativeui

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.sys.nativeui.navigationBar.NavigationBarNMM
import info.bagen.rust.plaoc.microService.sys.nativeui.safeArea.SafeAreaNMM
import info.bagen.rust.plaoc.microService.sys.nativeui.splashScreen.SplashScreenNMM
import info.bagen.rust.plaoc.microService.sys.nativeui.statusBar.StatusBarNMM
import info.bagen.rust.plaoc.microService.sys.nativeui.virtualKeyboard.VirtualKeyboardNMM

class NativeUiNMM : NativeMicroModule("nativeui.sys.dweb") {
    private val navigationBarNMM = NavigationBarNMM()
    private val statusBarNMM = StatusBarNMM()
    private val safeAreaNMM = SafeAreaNMM()
    private val virtualKeyboardNMM = VirtualKeyboardNMM()
    private  val splashScreenNMM = SplashScreenNMM()
    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        bootstrapContext.dns.install(navigationBarNMM)
        bootstrapContext.dns.install(statusBarNMM)
        bootstrapContext.dns.install(safeAreaNMM)
        bootstrapContext.dns.install(virtualKeyboardNMM)
        bootstrapContext.dns.install(splashScreenNMM)
    }

    override suspend fun _shutdown() {
    }
}