package info.bagen.dwebbrowser.microService.browser.desk

import androidx.compose.runtime.compositionLocalOf
import org.dweb_browser.dwebview.DWebView

val LocalInstallList = compositionLocalOf<MutableList<DeskAppMetaData>> {
  noLocalProvidedFor("LocalInstallList")
}

val LocalOpenList = compositionLocalOf<MutableList<DeskAppMetaData>> {
  noLocalProvidedFor("LocalOpenList")
}

val LocalDesktopView = compositionLocalOf<DeskController.MainDwebView> {
  noLocalProvidedFor("DesktopView")
}

internal fun noLocalProvidedFor(name: String): Nothing {
  error("[Desk]CompositionLocal $name not present")
}