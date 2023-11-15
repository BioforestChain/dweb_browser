package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme
import platform.UIKit.UIScreen

abstract class PureViewController : IPureViewController {
  open val createSignal = Signal<IPureViewCreateParams>()
  override val onCreate by lazy { createSignal.toListener() }
  open val destroySignal = Signal<Unit>()
  override val onDestroy by lazy { destroySignal.toListener() }
  open val touchSignal = Signal<TouchEvent>()
  override val onTouch by lazy { touchSignal.toListener() }
  fun getContent() = ComposeUIViewController {
    CompositionLocalProvider(LocalPureViewBox provides PureViewBox(UIScreen.mainScreen).also {
      it.setUiViewController(
        LocalUIViewController.current
      )
    }) {
      DwebBrowserAppTheme {
        for (content in contents) {
          content()
        }
      }
    }
  }

  override val contents = mutableStateListOf<@Composable () -> Unit>();
}

