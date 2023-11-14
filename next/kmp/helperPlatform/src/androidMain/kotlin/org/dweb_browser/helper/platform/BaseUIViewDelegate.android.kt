package org.dweb_browser.helper.platform

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.android.BaseActivity
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme

abstract class PureViewController : BaseActivity(), IPureViewController {
  private val createSignal = Signal<IPureViewCreateParams>()
  override val onCreate = createSignal.toListener()
  private val destroySignal = SimpleSignal()
  override val onDestroy = destroySignal.toListener()
  private val stopSignal = SimpleSignal()
  override val onStop = stopSignal.toListener()
  private val resumeSignal = SimpleSignal()
  override val onResume = resumeSignal.toListener()
  private val touchSignal = Signal<TouchEvent>()
  override val onTouch = touchSignal.toListener()
  final override fun onCreate(savedInstanceState: Bundle?) {
    lifecycleScope.launch {
      createSignal.emit(PureViewCreateParams(intent))
    }
    super.onCreate(savedInstanceState)
  }

  final override fun onStop() {
    lifecycleScope.launch {
      stopSignal.emit()
    }
    super.onStop()
  }

  final override fun onResume() {
    lifecycleScope.launch {
      resumeSignal.emit()
    }
    super.onResume()
  }

  final override fun onDestroy() {
    lifecycleScope.launch {
      destroySignal.emit()
    }
    super.onDestroy()
  }

  final override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
    lifecycleScope.launch {
      touchSignal.emit(
        TouchEvent(
          ev.x,
          ev.y,
          window.decorView.width.toFloat(),
          window.decorView.height.toFloat()
        )
      )
    }
    return super.dispatchTouchEvent(ev)
  }

  final override fun setContent(content: @Composable () -> Unit) {
    (this as ComponentActivity).setContent {
      CompositionLocalProvider(LocalPlatformViewController provides PlatformViewController(this)) {
        DwebBrowserAppTheme {
//          WindowCompat.getInsetsController(window, window.decorView)
//            .isAppearanceLightStatusBars = !isSystemInDarkTheme() // 设置状态栏颜色跟着主题走
          content()
        }
      }
    }
  }
}

fun Bundle.toMap() = mapOf(*keySet().map {
  Pair(it, get(it))
}.toTypedArray())

class PureViewCreateParams(
  val intent: Intent
) : Map<String, Any?> by (intent.extras ?: Bundle()).toMap(), IPureViewCreateParams {
  override fun getString(key: String): String? = get(key).let { require(it is String?);it }
  override fun getInt(key: String): Int? = get(key).let { require(it is Int?);it }
  override fun getFloat(key: String): Float? = get(key).let { require(it is Float?);it }
}