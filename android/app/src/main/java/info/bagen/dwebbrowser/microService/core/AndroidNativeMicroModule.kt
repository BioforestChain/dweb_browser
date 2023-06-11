package info.bagen.dwebbrowser.microService.core


import androidx.activity.ComponentActivity
import info.bagen.dwebbrowser.base.BaseActivity
import org.dweb_browser.helper.*
import info.bagen.dwebbrowser.microService.helper.Mmid

abstract class AndroidNativeMicroModule(override val mmid: Mmid) : NativeMicroModule(mmid) {

    companion object {
        //  管理所有的activity
        private val activityMap: MutableMap<Mmid, ComponentActivity> = mutableMapOf()
    }



    protected fun getActivity(mmid: Mmid): ComponentActivity? {
        return activityMap[mmid]
    }
    
    abstract fun openActivity(remoteMmid: Mmid)

    protected val activitySignal = Signal<MmidActivityArgs>()
    private fun onActivity(cb: Callback<MmidActivityArgs>) = activitySignal.listen(cb)

    init {
        // listen add activity
        onActivity { (mmid, activity) ->
            activityMap[mmid] = activity
            // listen self destroy  activity
            activity.onDestroyActivity {
                activityMap.remove(mmid)
            }
            return@onActivity true
        }

    }

}

typealias MmidActivityArgs = Pair<Mmid, BaseActivity>