package info.bagen.rust.plaoc.microService.sys.plugin.share

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.helper.PromiseOut

class ShareController(
//    val mmid: Mmid,
//    val localeMM: ShareNMM,
//    val remoteMM: MicroModule,
) {

    companion object {
         val controller = ShareController()
    }

    private var activityTask = PromiseOut<ShareActivity>()
    suspend fun waitActivityCreated() = activityTask.waitPromise()

    var activity: ShareActivity? = null
        set(value) {
            if (field == value) {
                return
            }
            field = value
            if (value == null) {
                activityTask = PromiseOut()
            } else {
                activityTask.resolve(value)
            }
        }



     fun startShareActivity(activity: Activity?) {
         val intent = Intent(activity,ShareActivity::class.java)
         intent.action = "info.bagen.dwebbrowser.share"
         intent.`package` = App.appContext.packageName
         intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
         activity?.startActivity(intent)
    }
}