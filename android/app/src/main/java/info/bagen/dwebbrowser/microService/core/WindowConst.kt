package info.bagen.dwebbrowser.microService.core

typealias UUID = String;

/**
 * 可变属性名称集合
 */
enum class WindowPropertyKeys(val key: String) {
  Any("*"),
  Title("title"),
  IconUrl("iconUrl"),
  Mode("mode"),
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

/**
 * 窗口的模式
 */
enum class WindowMode(val mode: String) {
  /**
   * 浮动模式，默认值
   */
  FLOATING("floating"),

  /**
   * 最大化
   */
  MAXIMIZE("maximize"),

  /**
   * 最小化
   */
  MINIMIZE("minimize"),

  /**
   * 全屏
   */
  FULLSCREEN("fullscreen"),

  /**
   * 画中画
   */
  PIP("picture-in-picture"),

  /**
   * 窗口关闭
   */
  CLOSED("closed"),
}


//  SPLIT_SCREEN, // 分屏模式
//  SNAP_LEFT, // 屏幕左侧对齐
//  SNAP_RIGHT, // 屏幕右侧对齐
//  CASCADE, // 级联模式
//  TILE_HORIZONTALLY, // 水平平铺
//  TILE_VERTICALLY, // 垂直平铺
//  FLOATING, // 浮动模式
//  PIP, // 画中画模式
//
//  CUSTOM // 自定义模式