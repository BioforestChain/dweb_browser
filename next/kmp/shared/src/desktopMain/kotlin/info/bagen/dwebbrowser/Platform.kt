package org.dweb_browser.shared

class DesktopPlatform: Platform {
    override val name: String = "Desktop"
}

actual fun getPlatform(): Platform = DesktopPlatform()
