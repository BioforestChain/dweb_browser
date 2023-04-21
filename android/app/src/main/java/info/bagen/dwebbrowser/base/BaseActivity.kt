package info.bagen.dwebbrowser.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import info.bagen.dwebbrowser.microService.helper.SimpleCallback
import info.bagen.dwebbrowser.microService.helper.SimpleSignal
import info.bagen.dwebbrowser.microService.helper.ioAsyncExceptionHandler
import info.bagen.dwebbrowser.ui.theme.RustApplicationTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

abstract class BaseActivity: ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initData()
    setContent {
      RustApplicationTheme {
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
          !isSystemInDarkTheme() // 设置状态栏颜色跟着主题走
        InitViews()
      }
    }
  }

   open fun initData() {} // 初始化数据，或者注册监听

  @Composable
  open fun InitViews() {}// 填充Compose布局


  private val onDestroySignal = SimpleSignal()

  fun onDestroyActivity(cb: SimpleCallback) = onDestroySignal.listen(cb)

  override fun onDestroy() {
    super.onDestroy()
    GlobalScope.launch(ioAsyncExceptionHandler) {
      onDestroySignal.emit()
    }
  }
}