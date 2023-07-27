package info.bagen.dwebbrowser.microService.browser.desktop.model

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.compositionLocalOf
import info.bagen.dwebbrowser.microService.browser.desktop.DeskAppMetaData

val LocalInstallList = compositionLocalOf<MutableList<DeskAppMetaData>> {
  noLocalProvidedFor("LocalInstallList")
}

val LocalOpenList = compositionLocalOf<MutableList<DeskAppMetaData>> {
  noLocalProvidedFor("LocalOpenList")
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