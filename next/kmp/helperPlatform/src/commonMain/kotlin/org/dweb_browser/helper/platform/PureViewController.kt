package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import org.dweb_browser.helper.Signal

interface IPureViewController {
  companion object

  val onCreate: Signal.Listener<IPureViewCreateParams>
  val onResume: Signal.Listener<Unit>
  val onStop: Signal.Listener<Unit>
  val onDestroy: Signal.Listener<Unit>

  //  fun setInteroperable(canTouch: Boolean)
  val onTouch: Signal.Listener<TouchEvent>
  suspend fun requestPermission(permission: String): Boolean

  //  val contents: MutableList<@Composable () -> Unit>
  val addContent: (content: @Composable () -> Unit) -> (() -> Boolean)
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
