package info.bagen.dwebbrowser.microService.browser.desk.view

import android.content.Context
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import info.bagen.dwebbrowser.microService.core.UUID
import info.bagen.dwebbrowser.microService.core.WindowController
import info.bagen.dwebbrowser.microService.core.WindowState
import info.bagen.dwebbrowser.microService.core.windowAdapterManager
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.microservice.help.MMID

@Composable
fun WindowPreviewer(
  wid: UUID = "preview",
  owner: MMID = "owner.preview.dweb",
  provider: MMID = "provider.preview.dweb",
  config: WindowController.() -> Unit = {},
  content: @Composable (modifier: Modifier, width: Float, height: Float, scale: Float) -> Unit = @Composable { _, _, _, _ -> },
) {
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  class PreviewWindowController(state: WindowState) : WindowController(state) {
    override val context: Context
      get() = context
    override val coroutineScope: CoroutineScope
      get() = scope
  }

  BoxWithConstraints(Modifier.fillMaxSize()) {
    val state = WindowState(
      wid = wid,
      owner = owner,
      provider = provider
    ).also {
      it.updateMutableBounds {
        width = maxWidth.value;
        height = maxHeight.value;
        left = 0f;
        top = 0f;
      }
    }
    val win = PreviewWindowController(state).also(config)
    windowAdapterManager.providers[wid] = content
    win.Render(maxWinWidth = maxWidth.value, maxWinHeight = maxHeight.value)
  }
}