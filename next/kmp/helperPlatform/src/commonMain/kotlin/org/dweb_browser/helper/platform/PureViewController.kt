package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.mapNotNull
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.compose.compositionChainOf

/**
 * 视图 后端
 */
interface IPureViewController {
  companion object

  val lifecycleScope: CoroutineScope

  val onCreate: Signal.Listener<IPureViewCreateParams>
  val onStart: Signal.Listener<Unit>
  val onResume: Signal.Listener<Unit>
  val onPause: Signal.Listener<Unit>
  val onStop: Signal.Listener<Unit>
  val onDestroy: Signal.Listener<Unit>

  //  fun setInteroperable(canTouch: Boolean)
  val onTouch: Signal.Listener<TouchEvent>

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

// 视图控制器
val LocalPureViewController = compositionChainOf<IPureViewController>("LocalPureViewController")

// region 视图回调事件 TODO 感觉这种回调视图的事件操作方法是不是不太好？
val LocalViewHookFlow = MutableSharedFlow<TrayEvent>()

// jsProcess 控制台回调
class LocalViewHookJsProcess {
  companion object {
    var isUse = false
    fun flow(): Flow<Boolean> {
      isUse = true
      return LocalViewHookFlow.mapNotNull {
        if (it == TrayEvent.JsProcess) true else null
      }
    }
  }

}

/**
 * 返回销毁事件回调
 * TODO fuck this
 */
val localViewHookExit = LocalViewHookFlow.mapNotNull {
  if (it == TrayEvent.Exit) true else null
}

enum class TrayEvent {
  Exit,
  JsProcess
}
// endregion