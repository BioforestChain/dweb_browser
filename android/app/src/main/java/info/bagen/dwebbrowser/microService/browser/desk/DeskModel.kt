package info.bagen.dwebbrowser.microService.browser.desk

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.compositionLocalOf
import org.dweb_browser.dwebview.DWebView

val LocalInstallList = compositionLocalOf<MutableList<DeskAppMetaData>> {
  noLocalProvidedFor("LocalInstallList")
}

val LocalOpenList = compositionLocalOf<MutableList<DeskAppMetaData>> {
  noLocalProvidedFor("LocalOpenList")
}

val LocalDesktopView = compositionLocalOf<DWebView> {
  noLocalProvidedFor("DesktopView")
}

private fun noLocalProvidedFor(name: String): Nothing {
  error("CompositionLocal $name not present")
}

val LocalDrawerManager = compositionLocalOf {
  DrawerManager()
}

class DrawerManager {
  val visibleState: MutableTransitionState<Boolean> = MutableTransitionState(true)
  fun hide() {
    visibleState.targetState = false
  }

  fun show() {
    visibleState.targetState = true
  }
}