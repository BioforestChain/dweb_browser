package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf

class CompositionChain(val providers: Array<ProvidedValue<*>> = emptyArray()) {

  @Composable
  fun Provider(vararg values: ProvidedValue<*>, content: @Composable () -> Unit) {
    if (values.isNotEmpty()) {
      val nextChain = CompositionChain(providers + values)
      CompositionLocalProvider(
        values = nextChain.providers + (LocalCompositionChain provides nextChain),
        content
      )
    } else {
      CompositionLocalProvider(
        values = providers + (LocalCompositionChain provides this),
        content
      )
    }
  }
}

val LocalCompositionChain = compositionLocalOf { CompositionChain() }
