package org.dweb_browser.sys.window.core.helper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowState

class WindowNavigation(private val state: WindowState) {

  private val goBackFlow = MutableSharedFlow<Unit>()

  internal val pageStack by lazy { mutableStateListOf<@Composable WindowContentRenderScope.(Modifier) -> Unit>() }
  fun pushPage(pageContent: @Composable WindowContentRenderScope.(Modifier) -> Unit) {
    pageStack.add(pageContent)
  }

  private class GoBackRecord private constructor(
    val onBack: suspend () -> Unit,
    var enabled: Boolean,
    var uiScope: CoroutineScope,
  ) {
    companion object {
      private val WM = WeakHashMap<suspend () -> Unit, GoBackRecord>()
      fun from(onBack: suspend () -> Unit, enabled: Boolean, uiScope: CoroutineScope) =
        WM.getOrPut(onBack) { GoBackRecord(onBack, enabled, uiScope) }.also {
          it.enabled = enabled
          it.uiScope = uiScope
        }
    }
  }

  private val onBackRecords by lazy {
    mutableStateListOf<GoBackRecord>().also { records ->
      goBackFlow.collectIn {
        records.lastOrNull { it.enabled }?.apply {
          uiScope.launch {
            onBack()
          }
        }
      }
    }
  }

  @Composable
  fun GoBackHandler(
    enabled: Boolean = true, onBack: suspend () -> Unit,
  ) {
    val uiScope = rememberCoroutineScope()
    val onBackState = remember { mutableStateOf(onBack) }
    onBackState.value = onBack

    DisposableEffect(this, enabled, onBackState) {
      val record = GoBackRecord.from({
        onBackState.value()
      }, enabled, uiScope)
      onBackRecords.add(record)
      state.canGoBack = onBackRecords.any { it.enabled } // onBackRecords.size > 0
      onDispose {
        onBackRecords.remove(record)
        state.canGoBack = if (onBackRecords.isEmpty()) null else onBackRecords.any { it.enabled }
      }
    }
  }

  suspend fun emitGoBack() {
    goBackFlow.emit(Unit)
  }

  internal val goBackButtonStack = mutableStateListOf<String>()


  private val goForwardSignal = SimpleSignal()
  val onGoForward = goForwardSignal.toListener()

  @Composable
  fun GoForwardHandler(enabled: Boolean = true, onForward: () -> Unit) {
    state.canGoForward = enabled
    DisposableEffect(this, enabled) {
      val off = goForwardSignal.listen { if (enabled) onForward() }
      onDispose {
        state.canGoForward = null
        off()
      }
    }
  }


  suspend fun emitGoForward() {
    goForwardSignal.emit()
  }
}