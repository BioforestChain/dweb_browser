package org.dweb_browser.window.core.constant

/**
 * 可变属性名称集合
 */
enum class WindowPropertyKeys(val fieldName: String) {
  Any("*"),
  Title("title"),
  IconUrl("iconUrl"),
  IconMaskable("iconMaskable"),
  IconMonochrome("iconMonochrome"),
  Mode("mode"),
  CanGoBack("canGoBack"),
  CanGoForward("canGoForward"),
  Resizable("resizable"),
  Focus("focus"),
  ZIndex("zIndex"),
  Children("children"),
  Parent("parent"),
  Flashing("flashing"),
  FlashColor("flashColor"),
  ProgressBar("progressBar"),
  AlwaysOnTop("alwaysOnTop"),
  DesktopIndex("desktopIndex"),
  ScreenId("screenId"),
  TopBarOverlay("topBarOverlay"),
  BottomBarOverlay("bottomBarOverlay"),
  TopBarContentColor("topBarContentColor"),
  TopBarBackgroundColor("topBarBackgroundColor"),
  BottomBarContentColor("bottomBarContentColor"),
  BottomBarBackgroundColor("bottomBarBackgroundColor"),
  BottomBarTheme("bottomBarTheme"),
  ThemeColor("themeColor"),
  Bounds("bounds"),
  KeyboardInsetBottom("keyboardInsetBottom"),
  KeyboardOverlaysContent("KeyboardOverlaysContent"),
}