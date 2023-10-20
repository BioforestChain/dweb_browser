package info.bagen.dwebbrowser.microService.sys.share

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import org.dweb_browser.helper.PromiseOut

class ShareController {

  companion object {
    val controller = ShareController()
  }

  var activity: ShareActivity? = null

  private var activityResultLauncherTask =
    PromiseOut<ActivityResultLauncher<Intent>>()

  suspend fun waitActivityResultLauncherCreated() = activityResultLauncherTask.waitPromise()

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
}

internal class ShareBroadcastReceiver: BroadcastReceiver() {
  companion object {
    private var shareReceiver = false
    fun resetState() { shareReceiver = false }
    val shareState get() = shareReceiver
  }

  override fun onReceive(context: Context, intent: Intent) {
    shareReceiver = true
    /*val clickedComponent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT, ComponentName::class.java)
    } else {
      intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT)
    }*/
  }
}