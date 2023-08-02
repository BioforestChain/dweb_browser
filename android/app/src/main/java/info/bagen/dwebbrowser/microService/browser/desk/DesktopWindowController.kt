package info.bagen.dwebbrowser.microService.browser.desk

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import info.bagen.dwebbrowser.microService.core.WindowController
import info.bagen.dwebbrowser.microService.core.WindowState
import info.bagen.dwebbrowser.microService.core.windowAdapterManager
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SimpleCallback
import org.dweb_browser.helper.SimpleSignal

class DesktopWindowController(
  override val context: Context, private val winState: WindowState
) : WindowController() {
  override fun toJson() = winState

  val id = winState.wid;

  private val _blurSignal = SimpleSignal()
  val onBlur = _blurSignal.toListener()
  private val _focusSignal = SimpleSignal()
  val onFocus = _focusSignal.toListener()
  fun isFocused() = winState.focus
  suspend fun focus() {
    if (!winState.focus) {
      winState.focus = true
      _focusSignal.emit()
    }
  }

  suspend fun blur() {
    if (winState.focus) {
      winState.focus = false
      _blurSignal.emit()
    }
  }

  @Composable
  fun Render(modifier: Modifier = Modifier) {
    var winState by remember { mutableStateOf(this.winState, neverEqualPolicy()) }
    SideEffect {
      this@DesktopWindowController.winState.onChange {
        winState = this@DesktopWindowController.winState;
        Unit
      }
    }
    val coroutineScope = rememberCoroutineScope()
    val emitWinStateChange = { -> coroutineScope.launch { winState.emitChange() } }
    val density = LocalDensity.current

    ElevatedCard(
      modifier = winState.bounds
        .toModifier(modifier)
        .focusable(true),
      colors = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
      ),
      elevation = CardDefaults.elevatedCardElevation()
//      elevation = CardElevation()
    ) {
      Column {
        /// 标题栏
        Box(
          modifier = Modifier
            .background(MaterialTheme.colorScheme.onPrimaryContainer)
            .fillMaxWidth()
            /// 标题栏可拖动
            .pointerInput(Unit) {
              detectDragGestures { change, dragAmount ->
                change.consume()
                winState.bounds.left += dragAmount.x / density.density
                winState.bounds.top += dragAmount.y / density.density
                emitWinStateChange()
              }
            }
        ) {
          Text(
            text = this@DesktopWindowController.winState.title,
            style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onPrimary)
          )
        }
        Box(
          modifier = Modifier
            .fillMaxSize()
        ) {
          windowAdapterManager.providers[this@DesktopWindowController.winState.wid]?.also {
            it(Modifier.fillMaxSize())
          } ?: Text(
            "Op！视图被销毁了",
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.headlineLarge.copy(
              color = MaterialTheme.colorScheme.error,
              background = MaterialTheme.colorScheme.errorContainer
            )
          )
        }
      }
    }
  }
}