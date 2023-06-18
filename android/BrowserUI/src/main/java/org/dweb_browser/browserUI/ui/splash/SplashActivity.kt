package org.dweb_browser.browserUI.ui.splash

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.dweb_browser.browserUI.ui.theme.BrowserUITheme

@SuppressLint("CustomSplashScreen")
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
    setContent { BrowserUITheme { SplashView(paths = list) } }
  }
}
