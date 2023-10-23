package info.bagen.dwebbrowser.microService.sys.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import info.bagen.dwebbrowser.App
import org.dweb_browser.helper.PromiseOut

class PermissionController {

  companion object {
    val controller = PermissionController()
  }
  val showDialog = mutableStateOf(false)
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

  fun openAppSettings() {
    val i = Intent()
    i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    i.addCategory(Intent.CATEGORY_DEFAULT)
    i.data = Uri.parse("package:" + App.appContext.packageName)
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
    i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    App.appContext.startActivity(i)
  }
}