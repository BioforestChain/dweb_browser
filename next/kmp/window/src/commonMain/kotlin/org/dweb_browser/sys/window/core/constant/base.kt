package org.dweb_browser.sys.window.core.constant

//import android.annotation.SuppressLint
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.compose.compositionChainOf

val debugWindow = Debugger("window")

val LocalWindowMM = compositionChainOf<MicroModule.Runtime>("Window MicroModule")

//@SuppressLint("ExperimentalAnnotationRetention")
@RequiresOptIn(
  level = RequiresOptIn.Level.WARNING,
  message = "This API is low-level in windows.sys.dweb and should be used with caution. You are advised to use service interfaces such as `tryCloseOrHide` and `closeRoot` instead."
)
public annotation class LowLevelWindowAPI