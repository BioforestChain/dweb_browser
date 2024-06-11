package org.dweb_browser.browser.desk

import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.compose.toComposableHelper

enum class TASKBAR_PROPERTY_KEY(val propertyKey: String) {
  LayoutX("x"),
  LayoutY("y"),
  LayoutWidth("width"),
  LayoutHeight("height"),
  LayoutTopPadding("topPadding"),
  LayoutLeftPadding("leftPadding"),
  FloatActivityState("float"),
  Dragging("dragging"),
}

/**
 * 用于和 Service 之间的交互，显示隐藏等操作
 */
class TaskbarState() {
  internal val observable = Observable<TASKBAR_PROPERTY_KEY>()
  val composableHelper by lazy { observable.toComposableHelper(this@TaskbarState) }
  var layoutX by observable.observe(TASKBAR_PROPERTY_KEY.LayoutX, Float.NaN)
  var layoutY by observable.observe(TASKBAR_PROPERTY_KEY.LayoutY, Float.NaN)
  var layoutWidth by observable.observe(TASKBAR_PROPERTY_KEY.LayoutWidth, 55f)
  var layoutHeight by observable.observe(TASKBAR_PROPERTY_KEY.LayoutHeight, 55f)
  var layoutTopPadding by observable.observe(TASKBAR_PROPERTY_KEY.LayoutTopPadding, 0f)
  var layoutLeftPadding by observable.observe(TASKBAR_PROPERTY_KEY.LayoutLeftPadding, 0f)
  var floatActivityState by observable.observe(TASKBAR_PROPERTY_KEY.FloatActivityState, false)

  /**
   * 专门用于桌面端的拖拽
   */
  var taskbarDragging by observable.observe(TASKBAR_PROPERTY_KEY.Dragging, false)
}

