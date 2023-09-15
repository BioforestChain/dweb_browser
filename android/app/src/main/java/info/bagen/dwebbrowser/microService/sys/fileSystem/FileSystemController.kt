package info.bagen.dwebbrowser.microService.sys.fileSystem

import org.dweb_browser.helper.PromiseOut

internal class FileSystemController {

  companion object {
    val controller = FileSystemController()
  }

  private var activityResultLauncherTask = PromiseOut<Boolean>()
  suspend fun waitPermissionGrants() = activityResultLauncherTask.waitPromise()

  var granted: Boolean? = null
    set(value) {
      if (field == value) return
      field = value
      if (value == null) {
        activityResultLauncherTask = PromiseOut()
      } else {
        activityResultLauncherTask.resolve(value)
      }
    }
}