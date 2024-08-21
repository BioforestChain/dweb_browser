package org.dweb_browser.sys.window.helper

// TODO 这里应该使用 WindowInsets#getRoundedCorner 来获得真实的物理圆角
/**
 * 获取窗口的圆角
 * 根据不同平台不同设备，提供窗口的圆角
 */
expect fun getWindowControllerBorderRounded(isMaximize: Boolean): WindowFrameStyle.CornerRadius
