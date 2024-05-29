package org.dweb_browser.helper

expect open class EnvSwitchCore internal constructor() {
  fun add(switch: String, value: String = "true")
  fun isEnabled(switch: String): Boolean
  fun get(switch: String): String
}

class EnvSwitch : EnvSwitchCore() {
  fun init(switch: String, defaultValue: () -> String) = get(switch).isEmpty().trueAlso {
    add(switch, defaultValue())
  }

  fun init(switch: ENV_SWITCH_KEY, defaultValue: () -> String) = init(switch.key, defaultValue)
  fun add(switch: ENV_SWITCH_KEY, value: String) = add(switch.key, value)
  fun isEnabled(switch: ENV_SWITCH_KEY) = isEnabled(switch.key)
  fun get(switch: ENV_SWITCH_KEY) = get(switch.key)
}


val envSwitch = EnvSwitch()

enum class ENV_SWITCH_KEY(val key: String) {
  DWEBVIEW_ENABLE_TRANSPARENT_BACKGROUND("dwebview-enable-transparent-background"),
  DESKTOP_DEV_URL("desktop-dev-url"),
  DESKTOP_DEVTOOLS("desktop-devtools"),
  TASKBAR_DEV_URL("taskbar-dev-url"),
  TASKBAR_DEVTOOLS("taskbar-devtools"),
  JS_PROCESS_DEVTOOLS("js-process-devtools"),
  ALL_WINDOW_DEVTOOLS("*-window-devtools"),
  DWEBVIEW_JS_CONSOLE("dwebview-js-console"),
  DESKTOP_STYLE_COMPOSE("destktop-style-compose"),
  ;
}