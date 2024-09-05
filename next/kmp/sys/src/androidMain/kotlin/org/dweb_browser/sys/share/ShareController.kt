package org.dweb_browser.sys.share

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.http.router.ResponseException
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SignalCallback
import org.dweb_browser.helper.getAppContextUnsafe

class ShareController {

  companion object {
    val controller = ShareController()
  }

  var activity: ShareActivity? = null

  private var activityResultLauncherTask =
    CompletableDeferred<ActivityResultLauncher<Intent>>()

  suspend fun waitActivityResultLauncherCreated() = activityResultLauncherTask.await()

  val getShareSignal = Signal<ResponseException>()
  fun getShareData(cb: SignalCallback<ResponseException>) = getShareSignal.listen(cb)

  var shareLauncher: ActivityResultLauncher<Intent>? = null
    set(value) {
      if (field == value) {
        return
      }
      field = value
      if (value == null) {
        activityResultLauncherTask = CompletableDeferred()
      } else {
        activityResultLauncherTask.complete(value)
      }
    }

  fun openActivity() {
    val context = getAppContextUnsafe()
    val intent = Intent(context, ShareActivity::class.java)
    intent.action = "${context.packageName}.share"
    intent.`package` = context.packageName
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    context.startActivity(intent)
  }
}