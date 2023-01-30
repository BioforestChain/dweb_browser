package info.bagen.libappmgr.ui.splash

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import info.bagen.libappmgr.ui.theme.AppMgrTheme

class SplashActivity : ComponentActivity() {
    companion object {
        const val SPLASH_LIST = "splash_list"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val list = arrayListOf<String>()
        intent?.let {
            it.getCharSequenceArrayListExtra(SPLASH_LIST)?.forEach { cs ->
                list.add(cs.toString())
            }
        }
        setContent {
            AppMgrTheme {
                SplashView(
                    paths = list, activeColor = Color.White, inactiveColor = Color.White.copy(0.5f)
                )
            }
        }
    }
}
