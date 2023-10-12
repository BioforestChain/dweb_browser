package org.dweb_browser.sys.window.core.constant

import androidx.compose.runtime.compositionLocalOf
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.compose.noLocalProvidedFor

val debugWindow = Debugger("window")

val LocalWindowMM = compositionLocalOf<MicroModule> {
  noLocalProvidedFor("Window MicroModule")
}