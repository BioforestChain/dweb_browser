package info.bagen.dwebbrowser

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable

class DesktopPlatform: Platform {
    override val name: String = "Desktop"
}

actual fun getPlatform(): Platform = DesktopPlatform()
