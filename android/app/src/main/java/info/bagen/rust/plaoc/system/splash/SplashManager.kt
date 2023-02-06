package info.bagen.rust.plaoc.system.splash

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import info.bagen.libappmgr.ui.splash.SplashActivity
import info.bagen.libappmgr.ui.splash.SplashView
import info.bagen.rust.plaoc.App

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
    fun loadSplashView(
        list: ArrayList<String>,
        activeColor: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
        inactiveColor: Color = activeColor.copy(ContentAlpha.disabled),
        indicatorWidth: Dp = 8.dp
    ) {
        SplashView(paths = list, activeColor, inactiveColor, indicatorWidth)
    }
}
