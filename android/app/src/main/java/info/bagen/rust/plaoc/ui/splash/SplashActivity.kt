package info.bagen.rust.plaoc.ui.splash

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme

class SplashActivity : AppCompatActivity() {
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
      RustApplicationTheme {
        SplashView(
          paths = list, activeColor = Color.White, inactiveColor = Color.White.copy(0.5f)
        )
      }
    }
  }
}
