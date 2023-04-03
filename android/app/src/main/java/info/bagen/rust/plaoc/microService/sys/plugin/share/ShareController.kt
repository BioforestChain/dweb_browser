package info.bagen.rust.plaoc.microService.sys.plugin.share

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import info.bagen.rust.plaoc.microService.helper.PromiseOut

class ShareController() {

    companion object {
         val controller = ShareController()
    }
    var activity: ShareActivity? = null

    private var activityResultLauncherTask = PromiseOut<ActivityResultLauncher<Intent>>()
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