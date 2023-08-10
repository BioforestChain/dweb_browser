package info.bagen.dwebbrowser.microService.browser.desk

import androidx.compose.runtime.compositionLocalOf

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