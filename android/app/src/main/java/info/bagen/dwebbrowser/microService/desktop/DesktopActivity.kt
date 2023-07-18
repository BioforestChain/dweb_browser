package info.bagen.dwebbrowser.microService.desktop

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import info.bagen.dwebbrowser.microService.desktop.ui.DesktopMainView
import info.bagen.dwebbrowser.ui.theme.DwebBrowserAppTheme

class DesktopActivity: AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      DwebBrowserAppTheme {
        SideEffect { WindowCompat.setDecorFitsSystemWindows(window, false) }
        DesktopMainView()
      }
    }
  }
}