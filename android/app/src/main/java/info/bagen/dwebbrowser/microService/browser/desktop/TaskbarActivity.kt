package info.bagen.dwebbrowser.microService.browser.desktop

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.base.ActivityBlurHelper
import info.bagen.dwebbrowser.ui.theme.DwebBrowserAppTheme

class TaskbarActivity : ComponentActivity() {
  private val blurHelper = ActivityBlurHelper(this)

  @SuppressLint("UseCompatLoadingForDrawables")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val density = resources.displayMetrics.density
    setContent {
      // val density = LocalDensity.current.density
      /// 改变窗口大小 和 一些属性
      window.setLayout((80 * density).toInt(), (200 * density).toInt());
      window.attributes = window.attributes.also { attributes ->
        /// 禁用模态窗口模式，使得点击可以向下穿透
        attributes.flags =
          WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        /// 将视图对其到右侧中央
        attributes.gravity = Gravity.CENTER_VERTICAL or Gravity.END;
      }

      DwebBrowserAppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
          OutlinedButton(onClick = { /*TODO*/ }) {
            Text(
              text = "桌面",
              style = MaterialTheme.typography.titleLarge,
              textAlign = TextAlign.Center
            )
          }
        }
      }
    }

    /// 启用模糊
    blurHelper.config(
      backgroundBlurRadius = (10 * resources.displayMetrics.density).toInt(),
      windowBackgroundDrawable = getDrawable(R.drawable.taskbar_window_background)
    )
  }
}
