package org.dweb_browser.sys.share

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.Callback
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.Signal

class ShareController {

  companion object {
    val controller = ShareController()
  }

  var activity: ShareActivity? = null

  private var activityResultLauncherTask =
    PromiseOut<ActivityResultLauncher<Intent>>()

  suspend fun waitActivityResultLauncherCreated() = activityResultLauncherTask.waitPromise()

  val getShareSignal = Signal<String>()
  fun getShareData(cb: Callback<String>) = getShareSignal.listen(cb)

  var shareLauncher: ActivityResultLauncher<Intent>? = null
    set(value) {
      if (field == value) {
        return
      }
      field = value
      if (value == null) {
        activityResultLauncherTask = PromiseOut()
      } else {
        activityResultLauncherTask.resolve(value)
      }
    }

  fun openActivity() {
    val context = NativeMicroModule.getAppContext()
    val intent = Intent(context, ShareActivity::class.java)
    intent.action = "${context.packageName}.share"
    intent.`package` = context.packageName
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    context.startActivity(intent)
  }
}