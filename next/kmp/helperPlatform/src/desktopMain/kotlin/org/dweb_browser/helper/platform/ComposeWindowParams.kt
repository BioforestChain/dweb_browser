package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.withScope

class ComposeWindowParams(
  private val pvc: PureViewController, val content: @Composable FrameWindowScope.() -> Unit
) {
  class CacheWindowState(
    override var isMinimized: Boolean,
    override var placement: WindowPlacement,
    override var position: WindowPosition,
    override var size: DpSize
  ) : WindowState

  internal var state: WindowState = CacheWindowState(
    isMinimized = false, placement = WindowPlacement.Floating,
    position = WindowPosition.PlatformDefault,
    size = DpSize(800.dp, 600.dp),
  )
  var isMinimized
    get() = state.isMinimized
    set(value) {
      state.isMinimized = value
    }
  var placement
    get() = state.placement
    set(value) {
      state.placement = value
    }
  var position
    get() = state.position
    set(value) {
      state.position = value
    }
  var size
    get() = state.size
    set(value) {
      state.size = value
    }

  var onCloseRequest by mutableStateOf<() -> Unit>({})
  var visible by mutableStateOf<Boolean>(true)
  var title by mutableStateOf<String>("Untitled")
  var icon by mutableStateOf<Painter?>(null)
  var undecorated by mutableStateOf<Boolean>(false)
  var transparent by mutableStateOf<Boolean>(false)
  var resizable by mutableStateOf<Boolean>(true)
  var enabled by mutableStateOf<Boolean>(true)
  var focusable by mutableStateOf<Boolean>(true)
  var alwaysOnTop by mutableStateOf<Boolean>(false)
  var onPreviewKeyEvent by mutableStateOf<(KeyEvent) -> Boolean>({ false })
  var onKeyEvent by mutableStateOf<(KeyEvent) -> Boolean>({ false })

  val isOpened get() = PureViewController.windowRenders.contains(this)
  private val openCloseLock = Mutex()

  suspend fun openWindow() = openCloseLock.withLock {
    if (isOpened) {
      return@withLock
    }
    withScope(pvc.lifecycleScope) {
      pvc.createSignal.emit(pvc.createParams)
    }
    withScope(PureViewController.uiScope) {
      PureViewController.windowRenders.add(this@ComposeWindowParams)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  suspend fun closeWindow() = openCloseLock.withLock {
    if (!isOpened) {
      return@withLock
    }
    pvc.composeWindowStateFlow.resetReplayCache()
    withScope(pvc.lifecycleScope) {
      pvc.destroySignal.emitAndClear()
    }
    withScope(PureViewController.uiScope) {
      PureViewController.windowRenders.remove(this@ComposeWindowParams)
    }
  }
}