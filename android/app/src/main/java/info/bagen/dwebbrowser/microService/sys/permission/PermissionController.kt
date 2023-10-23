package info.bagen.dwebbrowser.microService.sys.permission

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.dweb_browser.helper.PromiseOut

class PermissionController {

  companion object {
    val controller = PermissionController()
  }
  val deniedPermission : MutableState<String?> = mutableStateOf(null)
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