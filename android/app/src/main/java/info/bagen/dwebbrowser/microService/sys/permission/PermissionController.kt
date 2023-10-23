package info.bagen.dwebbrowser.microService.sys.permission

import org.dweb_browser.helper.PromiseOut

class PermissionController {

  companion object {
    val controller = PermissionController()
  }

  private var grantResult = PromiseOut<Boolean>()
  suspend fun waitGrantResult() = grantResult.waitPromise()

  var granted: Boolean? = null
    set(value) {
      if (field == value) return
      field = value
      if (value == null) {
        grantResult = PromiseOut()
      } else {
        grantResult.resolve(value)
      }
    }
}