package org.dweb_browser.window.render

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.platform.PlatformViewController
import org.dweb_browser.microservice.help.types.MMID
import org.dweb_browser.window.core.WindowController
import org.dweb_browser.window.core.WindowRenderProvider
import org.dweb_browser.window.core.WindowState
import org.dweb_browser.window.core.constant.WindowConstants
import org.dweb_browser.window.core.createWindowAdapterManager

@Composable
fun WindowPreviewer(
  wid: UUID = "preview",
  owner: MMID = "owner.preview.dweb",
  provider: MMID = "provider.preview.dweb",
  config: WindowController.() -> Unit = {},
  winRender: @Composable @UiComposable() (BoxWithConstraintsScope.(win: WindowController) -> Unit) = { win ->
    win.Render(maxWinWidth = maxWidth.value, maxWinHeight = maxHeight.value)
  },
  content: WindowRenderProvider = @Composable { _ -> },
) {
  val scope = rememberCoroutineScope()
  val context = LocalContext.current


  class PreviewWindowController(state: WindowState) : WindowController(state) {
    override val viewController: PlatformViewController = PlatformViewController(context)
    override val coroutineScope: CoroutineScope
      get() = scope
  }

  BoxWithConstraints(Modifier.fillMaxSize()) {
    val state = WindowState(
      WindowConstants(
        wid = wid,
        owner = owner,
        ownerVersion = "0.0.0",
        provider = provider
      )
    ).also {
      it.updateMutableBounds {
        width = maxWidth.value;
        height = maxHeight.value;
        left = 0f;
        top = 0f;
      }
    }
    val win = PreviewWindowController(state).also(config)
    createWindowAdapterManager.renderProviders[wid] = content
    winRender(win)
  }
}