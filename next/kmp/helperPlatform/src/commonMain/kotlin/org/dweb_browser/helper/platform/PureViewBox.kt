package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.CoroutineScope

expect suspend fun IPureViewBox.Companion.from(viewController: IPureViewController): IPureViewBox

/**
 * 视图 前端
 */
interface IPureViewBox {
  companion object {}

  val lifecycleScope: CoroutineScope
  suspend fun getViewWidthPx(): Int;
  suspend fun getViewHeightPx(): Int;
  suspend fun getDisplayDensity(): Float;

//  val windowInsetsCompat: WindowInsets
//
//  val onResize: Signal.Listener<Unit>
}
//expect interface PlatformContext {
//  val coroutineScope: CoroutineScope
//}


@Composable
fun rememberPureViewBox() = LocalPureViewBox.current

val LocalPureViewBox =
  compositionLocalOf<IPureViewBox> { throw Exception("PureViewBox no providers") }
