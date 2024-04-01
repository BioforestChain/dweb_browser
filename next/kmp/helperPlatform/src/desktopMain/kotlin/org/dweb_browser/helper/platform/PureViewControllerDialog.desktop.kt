package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.CompositionChain
import org.dweb_browser.helper.compose.timesToInt
import org.dweb_browser.helper.compose.toIntSize
import org.dweb_browser.helper.compose.toSize
import java.awt.Dialog
import javax.swing.JDialog
import javax.swing.SwingUtilities


@Composable
fun PureViewController.ModalDialog(
  requestClose: () -> Unit,
  state: DesktopPureDialogState = DesktopPureDialogState.default,
  content: @Composable (JDialog) -> Unit
) {
  val viewBox = LocalPureViewBox.current
  val dialog = remember {
    ComposeDialog(getComposeWindowOrNull(),Dialog.ModalityType.APPLICATION_MODAL, ).apply {
      title = state.title
      isUndecorated = true
//      isTransparent = true
      defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
    }
  }
  // 绑定title
  LaunchedEffect(state.title) {
    SwingUtilities.invokeLater {
      dialog.title = state.title
    }
  }
  // 绑定宽高
  val density = LocalDensity.current.density
  val layoutDirection = LocalLayoutDirection.current
  val boundsReady = remember { CompletableDeferred<Unit>() }
  val safeAreaSpacePx  =viewBox.asDesktop() .currentViewControllerMaxBounds().timesToInt(density)
  LaunchedEffect(density,safeAreaSpacePx, layoutDirection, state.alignment, state.width, state.height) {
    val maxDialogIntSize = viewBox.getViewControllerMaxBoundsPx().toIntSize()
    val dialogWidth = state.width?.times(density)?.toInt() ?: maxDialogIntSize.width
    val dialogHeight = state.height?.times(density)?.toInt() ?: maxDialogIntSize.height
    val offset = state.alignment.align(
      size = IntSize(dialogWidth, dialogHeight),
      space = safeAreaSpacePx.toIntSize(),
      layoutDirection = layoutDirection
    )
    SwingUtilities.invokeLater {
      dialog.setBounds(offset.x + safeAreaSpacePx.top, offset.y+safeAreaSpacePx.left, dialogWidth, dialogHeight)
      boundsReady.complete(Unit)
    }
  }
  val uiScope = rememberCoroutineScope()
  DisposableEffect(dialog) {
    dialog.setContent {
      state.chain.Provider {
        content(dialog)
      }
    }
    val job = uiScope.launch {
      boundsReady.await()
      SwingUtilities.invokeLater {
        dialog.isVisible = true
      }
    }
    onDispose {
      job.cancel()
      SwingUtilities.invokeLater {
        dialog.removeAll()
        dialog.isVisible = false
      }
      requestClose()
    }
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
