package info.bagen.dwebbrowser.microService.sys.biometrics


import org.dweb_browser.helper.*
import java.io.Serializable

class BiometricsController {
    companion object {
        val biometricsController = BiometricsController()
    }

    var activity: BiometricsActivity? = null
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

    private var activityTask = PromiseOut<BiometricsActivity>()
    suspend fun waitActivityCreated() = activityTask.waitPromise()

//    private var activityResultLauncherTask = PromiseOut<ActivityResultLauncher<Intent>>()
//    suspend fun waitActivityResultLauncherCreated() = activityResultLauncherTask.waitPromise()
//
//    var biometricsLauncher: ActivityResultLauncher<Intent>? = null
//        set(value) {
//            if (field == value) {
//                return
//            }
//            field = value
//            if (value == null) {
//                activityResultLauncherTask = PromiseOut()
//            } else {
//                activityResultLauncherTask.resolve(value)
//            }
//        }
}

data class BiometricsData(
    val title: String?,
    val subtitle: String?,
    val description: String?,
    val useFallback: Boolean?,
    val negativeButtonText: String?,
) : Serializable