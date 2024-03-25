package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.helper.compose.CompositionChain
import org.dweb_browser.helper.compose.toIntSize
import javax.swing.JDialog


@Composable
fun PureViewController.ModalDialog(
  requestClose: () -> Unit,
  state: DesktopPureDialogState = DesktopPureDialogState.default,
  content: @Composable (JDialog) -> Unit
) {
  val viewBox = LocalPureViewBox.current
  val panel = remember { ComposePanel() }
  val dialog = remember(panel) {
    JDialog(getComposeWindowOrNull(), state.title, /* isModal */true).apply {
      isUndecorated = true
      defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
      background = java.awt.Color(0f, 0f, 0f, 0f)

      panel.preferredSize = size
      add(panel)
    }
  }
  // 绑定title
  LaunchedEffect(state.title) {
    dialog.title = state.title
  }
  // 绑定宽高
  val density = LocalDensity.current.density
  val layoutDirection = LocalLayoutDirection.current
  val boundsReady = remember { CompletableDeferred<Unit>() }
  LaunchedEffect(density, layoutDirection, state.alignment, state.width, state.height) {
    val displayIntSize = viewBox.getDisplaySizePx()
    val maxDialogIntSize = viewBox.getViewControllerMaxBoundsPx().toIntSize()
    val dialogWidth = state.width?.times(density)?.toInt() ?: maxDialogIntSize.width
    val dialogHeight = state.height?.times(density)?.toInt() ?: maxDialogIntSize.height
    val offset = state.alignment.align(
      size = IntSize(dialogWidth, dialogHeight),
      space = displayIntSize,
      layoutDirection = layoutDirection
    )
    dialog.setBounds(offset.x, offset.y, dialogWidth, dialogHeight)
    boundsReady.complete(Unit)
  }
  DisposableEffect(dialog) {
    onDispose {
      println("QAQ isVisible=false")
      dialog.removeAll()
      dialog.isVisible = false
      requestClose()
    }
  }
  LaunchedEffect(content, state) {
    panel.setContent {
      state.chain.Provider {
        content(dialog)
      }
    }
    boundsReady.await()
    dialog.isVisible = true
    println("QAQ isVisible=true")
  }
}

class DesktopPureDialogState(
  val chain: CompositionChain,
  title: String? = null,
  width: Float? = null,
  height: Float? = null,
  alignment: Alignment = Alignment.Center,
) {
  var title by mutableStateOf(title ?: "")
  var width by mutableStateOf(width)
  var height by mutableStateOf(height)
  var alignment by mutableStateOf(alignment)

  companion object {
    val default = DesktopPureDialogState(title = "", chain = CompositionChain())
  }
}
