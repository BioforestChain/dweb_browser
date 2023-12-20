package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.helper.compose.compositionChainOf

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
  compositionChainOf<IPureViewBox>("PureViewBox")
