package org.dweb_browser.window.core.constant

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

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

/**
 * 底部按钮的风格
 */
@JsonAdapter(WindowBottomBarTheme::class)
enum class WindowBottomBarTheme(val themeName: String) : JsonSerializer<WindowBottomBarTheme>,
  JsonDeserializer<WindowBottomBarTheme> {
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
    fun from(themeName: String) = values().firstOrNull { it.themeName == themeName } ?: Navigation
  }

  override fun serialize(
    src: WindowBottomBarTheme,
    typeOfSrc: Type,
    context: JsonSerializationContext?
  ) = JsonPrimitive(src.themeName)

  override fun deserialize(
    json: JsonElement,
    typeOfT: Type?,
    context: JsonDeserializationContext?
  ) = from(json.asString)

}
