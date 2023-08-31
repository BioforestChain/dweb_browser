package info.bagen.dwebbrowser.microService.browser.desk

import androidx.compose.runtime.compositionLocalOf
import info.bagen.dwebbrowser.microService.browser.desk.types.DeskAppMetaData
import org.dweb_browser.helper.android.noLocalProvidedFor

val LocalInstallList = compositionLocalOf<MutableList<DeskAppMetaData>> {
  noLocalProvidedFor("LocalInstallList")
}

val LocalOpenList = compositionLocalOf<MutableList<DeskAppMetaData>> {
  noLocalProvidedFor("LocalOpenList")
}

val LocalDesktopView = compositionLocalOf<DeskController.MainDwebView> {
  noLocalProvidedFor("DesktopView")
}
