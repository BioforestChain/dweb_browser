package org.dweb_browser.helper.compose

import androidx.compose.ui.focus.FocusRequester

/**
 * 用于判断是否显示隐私协议
 * TODO 移除这个东西，使用更加合理的方案解决问题
 */
val LocalFocusRequester = compositionChainOf<FocusRequester?>("FocusRequester") {
  null
}