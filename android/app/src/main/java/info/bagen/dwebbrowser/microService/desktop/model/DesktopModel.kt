package info.bagen.dwebbrowser.microService.desktop.model

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.compositionLocalOf
import info.bagen.dwebbrowser.microService.core.WindowAppInfo

val LocalInstallList = compositionLocalOf<MutableList<WindowAppInfo>> {
  noLocalProvidedFor("LocalInstallList")
}

val LocalOpenList = compositionLocalOf<MutableList<WindowAppInfo>> {
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