package info.bagen.dwebbrowser.microService.browser.desk

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import info.bagen.dwebbrowser.microService.core.WindowController
import info.bagen.dwebbrowser.microService.core.WindowState
import info.bagen.dwebbrowser.microService.core.windowAdapterManager
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SimpleSignal

class DesktopWindowController(
  override val context: Context, internal val state: WindowState
) : WindowController() {
  override fun toJson() = state

  val id = state.wid;

  private val _blurSignal = SimpleSignal()
  val onBlur = _blurSignal.toListener()
  private val _focusSignal = SimpleSignal()
  val onFocus = _focusSignal.toListener()
  fun isFocused() = state.focus
  suspend fun focus() {
    if (!state.focus) {
      state.focus = true
      _focusSignal.emit()
    }
  }

  suspend fun blur() {
    if (state.focus) {
      state.focus = false
      _blurSignal.emit()
    }
  }


  @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
  @Composable
  fun Render(modifier: Modifier = Modifier) {
    var winState by remember { mutableStateOf(this.state, neverEqualPolicy()) }
    LaunchedEffect(this.state) {
      launch {
        winState.onChange.toFlow().collect {
          winState = this@DesktopWindowController.state;
        }
      }
    }
    val coroutineScope = rememberCoroutineScope()
    val emitWinStateChange = { -> coroutineScope.launch { winState.emitChange() } }
    val density = LocalDensity.current

    ElevatedCard(
      onClick = {
        coroutineScope.launch {
          focus();
        }
      },
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
            text = this@DesktopWindowController.state.title,
            style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onPrimary)
          )
        }
        Box(
          modifier = Modifier
            .fillMaxSize()
//            .pointerInteropFilter {
//              !winState.focus
//            }
        ) {
          windowAdapterManager.providers[this@DesktopWindowController.state.wid]?.also {
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