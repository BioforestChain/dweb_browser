package info.bagen.rust.plaoc.microService.core


import androidx.appcompat.app.AppCompatActivity
import info.bagen.rust.plaoc.microService.helper.Callback
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.Signal

abstract class AndroidNativeMicroModule(override val mmid: Mmid) : NativeMicroModule(mmid) {

    companion object {
        //  管理所有的activity
        private val activityMap: MutableMap<Mmid, AppCompatActivity> = mutableMapOf()
    }

    private var topActivity: AppCompatActivity? = null

    // 负责拿到最顶层的activity，即用户当前层
    fun getTopActivity(): AppCompatActivity? {
        return topActivity
    }


    protected val activitySignal = Signal<MmidActivityArgs>()
    protected val onDestroySignal = Signal<Mmid>()

    protected fun getActivity(mmid: Mmid): AppCompatActivity? {
        return activityMap[mmid]
    }

    private fun onActivity(cb: Callback<MmidActivityArgs>) = activitySignal.listen(cb)
    private fun onDestroyActivity(cb: Callback<Mmid>) = onDestroySignal.listen(cb)

    init {
        // listen add activity
        onActivity { (mmid, activity) ->
            topActivity = activity
            activityMap[mmid] = activity
            return@onActivity true
        }
        // listen destroy activity
        onDestroyActivity { mmid ->
            activityMap.remove(mmid)
        }
    }

}

typealias MmidActivityArgs = Pair<Mmid, AppCompatActivity>