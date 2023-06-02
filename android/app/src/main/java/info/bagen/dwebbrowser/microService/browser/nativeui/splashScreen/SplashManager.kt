package info.bagen.dwebbrowser.microService.browser.nativeui.splashScreen

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.compose.runtime.Composable
import info.bagen.dwebbrowser.ui.splash.SplashActivity
import info.bagen.dwebbrowser.ui.splash.SplashView
import info.bagen.dwebbrowser.App

object SplashManager {
    val SPLASH_LIST = SplashActivity.SPLASH_LIST

    /**
     * 打开加载页
     * @param list 传入需要在加载页显示的内容
     */
    fun openSplashActivity(list: ArrayList<String>) {
        val intent = Intent(App.appContext, SplashActivity::class.java).apply {
            putExtra(SPLASH_LIST, list)
            flags = FLAG_ACTIVITY_NEW_TASK
        }
        App.appContext.startActivity(intent)
    }

    @Composable
    fun loadSplashView(list: ArrayList<String>) {
        SplashView(paths = list)
    }
}
