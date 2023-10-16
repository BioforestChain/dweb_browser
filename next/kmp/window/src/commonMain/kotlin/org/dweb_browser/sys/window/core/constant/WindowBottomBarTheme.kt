package org.dweb_browser.sys.window.core.constant

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.StringEnumSerializer
import org.dweb_browser.helper.getOrDefault

object WindowBottomBarThemeSerializer : StringEnumSerializer<WindowBottomBarTheme>(
  "WindowBottomBarTheme",
  WindowBottomBarTheme.ALL_VALUES,
  { themeName })

/**
 * 底部按钮的风格
 */
@Serializable(with = WindowBottomBarThemeSerializer::class)
enum class WindowBottomBarTheme(val themeName: String) {
  /**
   * 导航模式：较高,面向常见的网页,依次提供app-id+version(两行小字显示)、back-bottom、forword-bottom、unmax bottom(1)。点击app-id等效于点击顶部的titlebar展开的菜单栏(显示窗窗口信息、所属应用信息、一些设置功能(比如刷新页面、设置分辨率、设置UA、查看路径))
   */
  Navigation("navigation"),

  /**
   * 沉浸模式：较矮,只提供app-id+version的信息(一行小字)
   */
  Immersion("immersion"),
//  Status("custom"),
//  Status("status"),
  ;


  companion object {
    val ALL_VALUES = entries.associateBy { it.themeName }
    fun from(themeName: String) = ALL_VALUES.getOrDefault(themeName, Navigation)
  }


}
