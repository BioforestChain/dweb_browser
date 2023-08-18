package org.dweb_browser.window.core.constant

/**
 * 窗口被接管时的上下文
 */
data class WindowsManagerScope(val doDestroy: () -> Unit)