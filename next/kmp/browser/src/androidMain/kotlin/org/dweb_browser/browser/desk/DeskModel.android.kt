package org.dweb_browser.browser.desk

import androidx.compose.runtime.compositionLocalOf
import org.dweb_browser.helper.compose.noLocalProvidedFor


val LocalDesktopView = compositionLocalOf<DesktopController.MainDwebView> {
  noLocalProvidedFor("DesktopView")
}
