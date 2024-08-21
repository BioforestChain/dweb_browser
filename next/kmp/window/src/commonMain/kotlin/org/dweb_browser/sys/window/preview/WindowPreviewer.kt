package org.dweb_browser.sys.window.preview

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.platform.rememberPureViewBox
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowRenderProvider
import org.dweb_browser.sys.window.core.WindowState
import org.dweb_browser.sys.window.core.constant.WindowConstants
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.render.Prepare
import org.dweb_browser.sys.window.render.WindowRender

@Composable
fun WindowPreviewer(
  wid: UUID = "preview",
  owner: MMID = "owner.preview.dweb",
  provider: MMID = "provider.preview.dweb",
  config: WindowController.() -> Unit = {},
  modifier: Modifier = Modifier,
  winRender: @Composable @UiComposable() (BoxWithConstraintsScope.(win: WindowController) -> Unit) = { win ->
    win.Prepare(winMaxWidth = maxWidth.value, winMaxHeight = maxHeight.value) {
      win.WindowRender(Modifier)
    }
  },
  content: WindowRenderProvider = @Composable { _ -> },
) {
  val scope = rememberCoroutineScope()


  class PreviewWindowController(
    state: WindowState,
    viewBox: IPureViewBox,
  ) : WindowController(state, viewBox) {
    override val lifecycleScope: CoroutineScope
      get() = scope
  }

  BoxWithConstraints(modifier.fillMaxSize()) {
    val state = WindowState(
      WindowConstants(
        wid = wid, owner = owner, ownerVersion = "0.0.0", provider = provider
      )
    ).also {
      it.updateMutableBounds {
        width = maxWidth.value;
        height = maxHeight.value;
        x = 0f;
        y = 0f;
      }
    }
    val platformViewController = rememberPureViewBox()
    val win =
      remember(state) { PreviewWindowController(state, platformViewController) }.also(config)
    windowAdapterManager.provideRender(wid, content)
    winRender(win)
  }
}