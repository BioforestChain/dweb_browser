package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.helper.Signal

/**
 * 视图 后端
 */
interface IPureViewController {
  companion object

  val lifecycleScope: CoroutineScope

  val onCreate: Signal.Listener<IPureViewCreateParams>
  val onResume: Signal.Listener<Unit>
  val onPause: Signal.Listener<Unit>
  val onStop: Signal.Listener<Unit>
  val onDestroy: Signal.Listener<Unit>

  //  fun setInteroperable(canTouch: Boolean)
  val onTouch: Signal.Listener<TouchEvent>
  suspend fun requestPermission(permission: String): Boolean

  fun getContents(): MutableList<@Composable () -> Unit>
  fun addContent(content: @Composable () -> Unit): () -> Boolean {
    getContents().add(content);
    return {
      getContents().remove(content)
    }
  }
}

/**
 * BaseUIViewDelegate 的 onTouch 事件参数
 *
 * x，y 都是想对 view 的一个坐标
 * 如果在 view 外部点击，当又被捕捉，那么此时 x、y 可能小于 0 或者大于 viewWidth，viewHeight
 */
data class TouchEvent(val x: Float, val y: Float, val viewWidth: Float, val viewHeight: Float)

interface IPureViewCreateParams : Map<String, Any?> {
  fun getString(key: String): String?
  fun getInt(key: String): Int?
  fun getFloat(key: String): Float?
  fun getBoolean(key: String): Boolean?
}

val LocalPureViewController = compositionLocalOf<IPureViewController> {
  noLocalProvidedFor("LocalPureViewController")
}