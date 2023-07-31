package info.bagen.dwebbrowser.microService.browser.desktop

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.base.ActivityBlurHelper
import info.bagen.dwebbrowser.ui.theme.DwebBrowserAppTheme


class TaskbarActivity : ComponentActivity() {
  private val blurHelper = ActivityBlurHelper(this)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      val density = LocalDensity.current;
      /// 启用模糊
      blurHelper.config(
        backgroundBlurRadius = (10 * density.density).toInt(),
        windowBackgroundDrawable = getDrawable(R.drawable.taskbar_window_background)
      )
      /// 改变窗口大小 和 一些属性
      window.setLayout((80 * density.density).toInt(), (200 * density.density).toInt());
      window.attributes = window.attributes.also { attributes ->
        /// 禁用模态窗口模式，使得点击可以向下穿透
        attributes.flags =
          (WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        /// 将视图对其到右侧中央
        attributes.gravity = Gravity.CENTER_VERTICAL or Gravity.END;
      }

      DwebBrowserAppTheme {
        Box(
          modifier = Modifier
            .fillMaxSize()
        ) {
          OutlinedButton(
            onClick = { /*TODO*/ }, modifier = Modifier
//              .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f))
          ) {
            Text(
              text = "桌面",
              style = MaterialTheme.typography.titleLarge.copy(background = Color.Transparent)
            )
          }
        }
      }
    }
  }

}
