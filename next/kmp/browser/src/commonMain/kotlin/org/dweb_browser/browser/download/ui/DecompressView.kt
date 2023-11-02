package org.dweb_browser.browser.download.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier

val LocalShowDecompress = compositionLocalOf { mutableStateOf(false) }

@Composable
fun DecompressView() {
  val show = LocalShowDecompress.current
  if (show.value) {
    Box(modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()) {
      Column(modifier = Modifier.fillMaxWidth()) {

      }
    }
  }
}