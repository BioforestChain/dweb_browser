package info.bagen.dwebbrowser.microService.core

typealias UUID = String;

/**
 * 可变属性名称集合
 */
enum class WindowPropertyKeys(val key: String) {
  Title("title"),
  IconUrl("iconUrl"),
  Fullscreen("fullscreen"),
  Maximize("maximize"),
  Minimize("minimize"),
  Resizable("resizable"),
  Focus("focus"),
  PictureInPicture("pictureInPicture"),
  ZIndex("zIndex"),
  Children("children"),
  Parent("parent"),
  Flashing("flashing"),
  FlashColor("flashColor"),
  ProgressBar("progressBar"),
  AlwaysOnTop("alwaysOnTop"),
  DesktopIndex("desktopIndex"),
  ScreenId("screenId"),
  OverlayTopBar("overlayTopBar"),
  OverlayBottomBar("overlayBottomBar"),
  TopBarContentColor("topBarContentColor"),
  TopBarBackgroundColor("topBarBackgroundColor"),
  BottomBarContentColor("bottomBarContentColor"),
  BottomBarBackgroundColor("bottomBarBackgroundColor"),
  ThemeColor("themeColor"),
  Bounds("bounds"),
}