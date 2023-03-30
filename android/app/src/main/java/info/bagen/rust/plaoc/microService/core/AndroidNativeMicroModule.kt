package info.bagen.rust.plaoc.microService.core


import androidx.appcompat.app.AppCompatActivity
import info.bagen.rust.plaoc.microService.helper.Callback
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.Signal
import info.bagen.rust.plaoc.microService.sys.mwebview.*

abstract class AndroidNativeMicroModule(override val mmid: Mmid) : NativeMicroModule(mmid) {
    open var topActivity: AppCompatActivity? = null

    companion object {
        private val activityMap: MutableMap<Mmid, AppCompatActivity> = mutableMapOf()
        val activityClassList = mutableListOf(
            MultiWebViewNMM.ActivityClass("", MultiWebViewPlaceholder1Activity::class.java),
            MultiWebViewNMM.ActivityClass("", MultiWebViewPlaceholder2Activity::class.java),
            MultiWebViewNMM.ActivityClass("", MultiWebViewPlaceholder3Activity::class.java),
            MultiWebViewNMM.ActivityClass("", MultiWebViewPlaceholder4Activity::class.java),
            MultiWebViewNMM.ActivityClass("", MultiWebViewPlaceholder5Activity::class.java),
        )
        val controllerMap = mutableMapOf<Mmid, MultiWebViewController>()


    }
    /**获取当前的controller, 只能给nativeUI 使用，因为他们是和mwebview绑定在一起的*/
    fun getCurrentWebViewController(mmid: Mmid): MultiWebViewController? {
        return controllerMap[mmid]
    }

    protected val activitySignal = Signal<MmidActivityArgs>()
    protected val onDestroySignal = Signal<Mmid>()

    protected fun getActivity(mmid: Mmid): AppCompatActivity? {
        return activityMap[mmid]
    }

    private fun onActivity(cb: Callback<MmidActivityArgs>) = activitySignal.listen(cb)
    private fun onDestroyActivity(cb: Callback<Mmid>) = onDestroySignal.listen(cb)

    init {
        onActivity { (mmid, activity) ->
            topActivity = activity
            activityMap[mmid] = activity
            return@onActivity true
        }
        onDestroyActivity { mmid ->
            activityMap.remove(mmid)
        }
    }

}

typealias MmidActivityArgs = Pair<Mmid, AppCompatActivity>