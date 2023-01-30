package info.bagen.libappmgr.utils

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import info.bagen.libappmgr.entity.AppInfo

/** 通过反射获取当前应用的Application、Context */
class AppContextUtil {

    companion object {
        var appInfoList: MutableList<AppInfo> = mutableStateListOf()

        val sInstance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            PlaocApplication()
        }

        private fun PlaocApplication(): Application? {
            try {
                var acThreadClass =
                    Class.forName("android.app.ActivityThread") ?: return null

                var acThreadMethod =
                    acThreadClass.getMethod("currentActivityThread") ?: return null

                acThreadMethod.isAccessible = true // 允许访问私有方法

                var activityThread = acThreadMethod.invoke(null)

                var appMethod =
                    activityThread.javaClass.getMethod("getApplication") ?: return null

                return appMethod.invoke(activityThread) as Application
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        fun showShortToastMessage(msg: String) {
            Toast.makeText(sInstance, msg, Toast.LENGTH_SHORT).show()
        }

        fun showLongToastMessage(msg: String) {
            Toast.makeText(sInstance, msg, Toast.LENGTH_LONG).show()
        }
    }
}
