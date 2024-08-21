package org.dweb_browser.sys.window.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.dweb_browser.sys.window.core.renderConfig.EffectWindowLayerStyleDelegate
import org.dweb_browser.sys.window.core.renderConfig.FrameDragDelegate

internal class WindowRenderConfig {
  /**
   * 基于拖拽，窗口移动
   */
  var frameMoveDelegate by mutableStateOf<FrameDragDelegate?>(null)

  /**
   * 基于拖拽，窗口从左下角进行resize
   */
  var frameLBResizeDelegate by mutableStateOf<FrameDragDelegate?>(null)

  /**
   * 基于拖拽，窗口从右下角进行resize
   */
  var frameRBResizeDelegate by mutableStateOf<FrameDragDelegate?>(null)

  /**
   * 是否使用操作系统原生的窗口图层，同时用以提供 bounds
   */
  var isSystemWindow by mutableStateOf(false)

  /**
   * 是否使用 compose 绘制窗口边框
   */
  var isWindowUseComposeFrame by mutableStateOf(true)


  var effectWindowLayerStyleDelegate by mutableStateOf<EffectWindowLayerStyleDelegate?>(null)
}
