package info.bagen.dwebbrowser.microService.sys.nativeui

import info.bagen.dwebbrowser.microService.core.BootstrapContext
import info.bagen.dwebbrowser.microService.core.NativeMicroModule
import info.bagen.dwebbrowser.microService.sys.nativeui.dwebServiceWorker.DwebServiceWorkerNMM
import info.bagen.dwebbrowser.microService.sys.nativeui.navigationBar.NavigationBarNMM
import info.bagen.dwebbrowser.microService.sys.nativeui.safeArea.SafeAreaNMM
import info.bagen.dwebbrowser.microService.sys.nativeui.splashScreen.SplashScreenNMM
import info.bagen.dwebbrowser.microService.sys.nativeui.statusBar.StatusBarNMM
import info.bagen.dwebbrowser.microService.sys.nativeui.virtualKeyboard.VirtualKeyboardNMM

class NativeUiNMM : NativeMicroModule("nativeui.sys.dweb") {
    private val navigationBarNMM = NavigationBarNMM()
    private val statusBarNMM = StatusBarNMM()
    private val safeAreaNMM = SafeAreaNMM()
    private val virtualKeyboardNMM = VirtualKeyboardNMM()
    private  val splashScreenNMM = SplashScreenNMM()
    private  val dwebServiceWorkerNMM = DwebServiceWorkerNMM()

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        bootstrapContext.dns.install(navigationBarNMM)
        bootstrapContext.dns.install(statusBarNMM)
        bootstrapContext.dns.install(safeAreaNMM)
        bootstrapContext.dns.install(virtualKeyboardNMM)
        bootstrapContext.dns.install(splashScreenNMM)
        bootstrapContext.dns.install(dwebServiceWorkerNMM)
    }

    override suspend fun _shutdown() {
    }
}