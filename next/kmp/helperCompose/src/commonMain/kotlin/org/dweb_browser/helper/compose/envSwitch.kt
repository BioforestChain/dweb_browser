package org.dweb_browser.helper.compose

import org.dweb_browser.helper.platform.EnvSwitchCore
import org.dweb_browser.helper.trueAlso

class EnvSwitch : EnvSwitchCore() {
  fun init(switch: String, defaultValue: () -> String) = get(switch).isEmpty().trueAlso {
    set(switch, defaultValue())
  }

  fun init(switch: ENV_SWITCH_KEY, defaultValue: () -> String) = init(switch.key, defaultValue)
  fun isEnabled(switch: ENV_SWITCH_KEY) = isEnabled(switch.key)
  fun get(switch: ENV_SWITCH_KEY) = get(switch.key)
  fun set(switch: ENV_SWITCH_KEY, value: String) = set(switch.key, value)
  fun enable(switch: ENV_SWITCH_KEY) {
    if (switch.experimental != null) {
      for (sameBranchSwitchKey in ENV_SWITCH_KEY.entries) {
        if (sameBranchSwitchKey !== switch && sameBranchSwitchKey.experimental?.brand == switch.experimental.brand) {
          disable(sameBranchSwitchKey)
        }
      }
    }

    set(switch.key, "true")
  }

  fun disable(switch: ENV_SWITCH_KEY) {
    remove(switch)
    /// 如果默认值是 true，那么不能用remove，得直接赋值 false
    if (isEnabled(switch)) {
      set(switch.key, "false")
    }
  }

  fun remove(switch: ENV_SWITCH_KEY) = remove(switch.key)

  fun watch(
    switch: ENV_SWITCH_KEY,
    initEvaluation: Boolean = true,
    block: suspend () -> Unit,
  ) = watch(switch.key, initEvaluation, block)
}

val envSwitch = EnvSwitch()

class ExperimentalKey(
  val title: SimpleI18nResource,
  val description: SimpleI18nResource,
  val brand: String,
  val disableVersion: String,
  val enableVersion: String,
)

enum class ENV_SWITCH_KEY(
  val key: String,
  /**
   * experimental 意味着可被用户配置。
   * 如果需要作为 experimental 项，请提供相关的配置信息
   */
  val experimental: ExperimentalKey? = null,
) {
  DWEBVIEW_ENABLE_TRANSPARENT_BACKGROUND("dwebview-enable-transparent-background"),
  DESKTOP_DEV_URL("desktop-dev-url"),
  DESKTOP_DEVTOOLS("desktop-devtools"),
  TASKBAR_DEV_URL("taskbar-dev-url"),
  TASKBAR_DEVTOOLS("taskbar-devtools"),
  JS_PROCESS_DEVTOOLS("js-process-devtools"),
  ALL_WINDOW_DEVTOOLS("*-window-devtools"),
  DWEBVIEW_JS_CONSOLE("dwebview-js-console"),
  DESKTOP_STYLE_COMPOSE(
    "destktop-style-compose",
    experimental = ExperimentalKey(
      title = SimpleI18nResource(
        Language.ZH to "桌面引擎2.0",
        Language.EN to "DesktopView Engine 2.0",
      ),
      description = SimpleI18nResource(
        Language.ZH to "使用新版桌面，获得更快的启动速度和更好的性能，欢迎使用体验。",
        Language.EN to "Using the new version desktop, you'll enjoy faster startup times and better performance. Welcome to try it out.",
      ),
      brand = "dweb-desktop",
      disableVersion = "1",
      enableVersion = "2",
    ),
  ),
  DWEBVIEW_PROFILE(
    "dwebview-profile",
    experimental = ExperimentalKey(
      title = SimpleI18nResource(
        Language.ZH to "模块数据隔离", Language.EN to "Module Data Isolation"
      ),
      description = SimpleI18nResource(
        Language.ZH to "如若启用该功能，意味着各个的模块将有自己的数据隔离区。请注意，请做好数据备份！一旦启用，模块将离开原先的的数据区域。",
        Language.EN to "Enabling this feature will create separate data isolation zones for every modules. Please note that data backups are essential! Once enabled, modules will be moved out of their original data zones.",
      ),
      brand = "dweb-module-profile",
      disableVersion = "0",
      enableVersion = "1",
    ),
  ),
  BROWSER_DOWNLOAD(
    "browser-download",
    experimental = ExperimentalKey(
      title = SimpleI18nResource(
        Language.ZH to "下载管理", Language.EN to "Download Manager"
      ),
      description = SimpleI18nResource(
        Language.ZH to "管理下载的内容",
        Language.EN to "Manage downloaded contents",
      ),
      brand = "dweb-browser-download",
      disableVersion = "0",
      enableVersion = "1",
    ),
  ),
  DWEBVIEW_PULLDOWNWEBMENU(
    "dwebview-pullwebmenu",
    experimental = ExperimentalKey(
      title = SimpleI18nResource(
        Language.ZH to "网页下拉快捷菜单", Language.EN to "Quick Menu On Web Top"
      ),
      description = SimpleI18nResource(
        Language.ZH to "启用该功能，则可以在网页滚动到顶部时下拉出快捷菜单，完成刷新网页，关闭标签页，新开标签页这些功能",
        Language.EN to "Enabling this feature allows you to pull down a quick menu when scrolling to the top of the webpage, where you can refresh the page, close a tab, or open a new tab.",
      ),
      brand = "dweb-browser-pullmenu",
      disableVersion = "1",
      enableVersion = "2",
    )
  )
//  SCAN_STD(
//    "lens-nmm",
//    experimental = ExperimentalKey(
//      title = SimpleI18nResource(
//        Language.ZH to "智能镜头", Language.EN to "Lens"
//      ),
//      description = SimpleI18nResource(
//        Language.ZH to "扫码、扫描、智能视觉模块.",
//        Language.EN to "Code scanning, visual scanning, and smart vision modules.",
//      ),
//      brand = "dweb-lens",
//      disableVersion = "0",
//      enableVersion = "1",
//    ),
//  ),
}