package info.bagen.rust.plaoc.microService.sys.plugin.share

import android.content.Intent
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



     fun startShareActivity() {
        App.startActivity(ShareActivity::class.java){
            it.action = "info.bagen.dwebbrowser.share"
            it.`package` = App.appContext.packageName
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        }
    }
}