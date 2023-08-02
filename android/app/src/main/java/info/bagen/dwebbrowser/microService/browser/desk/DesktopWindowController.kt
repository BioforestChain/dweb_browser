package info.bagen.dwebbrowser.microService.browser.desk

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import info.bagen.dwebbrowser.microService.core.WindowController
import info.bagen.dwebbrowser.microService.core.WindowState
import info.bagen.dwebbrowser.microService.core.windowAdapterManager

class DesktopWindowController(
  override val androidContext: Context, private val winState: WindowState
) : WindowController() {
  override fun toJson() = winState

  @Composable
  fun Render() {
    ElevatedCard(
      modifier = winState.bounds.toModifier(Modifier),
      colors = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
      ),
//      elevation = CardElevation()
    ) {
      Column {
        Box(
          modifier = Modifier
            .background(MaterialTheme.colorScheme.onPrimaryContainer)
            .fillMaxWidth()
        ) {
          Text(
            text = winState.title,
            style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onPrimary)
          )
        }
        Box(
          modifier = Modifier
            .fillMaxSize()
        ) {
          windowAdapterManager.providers[winState.wid]?.also {
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