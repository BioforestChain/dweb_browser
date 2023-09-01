package info.bagen.dwebbrowser.microService.browser.desk

import org.dweb_browser.helper.Observable

enum class TASKBAR_PROPERTY_KEY(val propertyKey: String) {
  LayoutX("x"),
  LayoutY("y"),
  LayoutWidth("width"),
  LayoutHeight("height"),
  LayoutTopPadding("topPadding"),
  LayoutLeftPadding("leftPadding"),
  FloatActivityState("float"),
}

/**
 * 用于和 Service 之间的交互，显示隐藏等操作
 */
class TaskbarState() {
  internal val observable = Observable<TASKBAR_PROPERTY_KEY>()
  var layoutX by observable.observe(TASKBAR_PROPERTY_KEY.LayoutX, Float.NaN)
  var layoutY by observable.observe(TASKBAR_PROPERTY_KEY.LayoutY, Float.NaN)
  var layoutWidth by observable.observe(TASKBAR_PROPERTY_KEY.LayoutWidth, 75f)
  var layoutHeight by observable.observe(TASKBAR_PROPERTY_KEY.LayoutHeight, 75f)
  var layoutTopPadding by observable.observe(TASKBAR_PROPERTY_KEY.LayoutTopPadding, 0f)
  var layoutLeftPadding by observable.observe(TASKBAR_PROPERTY_KEY.LayoutLeftPadding, 0f)
  var floatActivityState by observable.observe(TASKBAR_PROPERTY_KEY.FloatActivityState, false)
}

