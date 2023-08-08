package info.bagen.dwebbrowser.microService.core

import org.dweb_browser.helper.ChangeableMap

class WindowInstancesManager {
  val instances = ChangeableMap<UUID, WindowController>()

}

val windowInstancesManager = WindowInstancesManager()