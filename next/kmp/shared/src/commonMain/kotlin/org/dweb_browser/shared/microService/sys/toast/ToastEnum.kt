package org.dweb_browser.shared.microService.sys.toast



enum class DurationType(duration: Long) {
    SHORT(2000L), LONG(3500L)
}

enum class PositionType(val position: String) {
    TOP("top"), CENTER("center"), BOTTOM("bottom")
}