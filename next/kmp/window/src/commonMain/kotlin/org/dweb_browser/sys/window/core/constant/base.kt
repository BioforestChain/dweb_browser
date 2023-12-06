package org.dweb_browser.sys.window.core.constant

//import android.annotation.SuppressLint
import androidx.compose.runtime.compositionLocalOf
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.platform.noLocalProvidedFor

val debugWindow = Debugger("window")

val LocalWindowMM = compositionLocalOf<NativeMicroModule> {
  noLocalProvidedFor("Window MicroModule")
}

//@SuppressLint("ExperimentalAnnotationRetention")
@RequiresOptIn(
  level = RequiresOptIn.Level.WARNING,
  message = "This API is low-level in windows.sys.dweb and should be used with caution. You are advised to use service interfaces such as `tryCloseOrHide` and `closeRoot` instead."
)
public annotation class LowLevelWindowAPI