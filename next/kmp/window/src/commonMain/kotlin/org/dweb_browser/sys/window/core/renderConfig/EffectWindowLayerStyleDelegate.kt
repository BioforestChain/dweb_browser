package org.dweb_browser.sys.window.core.renderConfig

import androidx.compose.ui.unit.Dp
import org.dweb_browser.sys.window.helper.WindowFrameStyle

/**
 * 窗口图层样式：
 * 窗口透明度与窗口缩放比例
 * 这些值不在 窗口属性中，属于窗口渲染器直接提供
 *
 * 于是在与原生试图进行混合渲染的时候，它们需要知道这些上下文，从而做出相似的配合。
 * 目前主要是IOS在使用
 */
data class WindowLayerStyle(
  val scale: Float = 1f,
  val opacity: Float = 1f,
  val elevation: Dp,
  val cornerRadius: WindowFrameStyle.CornerRadius,
)

fun interface EffectWindowLayerStyleDelegate {
  fun effectStyle(windowLayerStyle: WindowLayerStyle)
}
