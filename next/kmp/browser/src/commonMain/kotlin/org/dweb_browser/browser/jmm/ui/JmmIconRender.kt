package org.dweb_browser.browser.jmm.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.desk.render.toDeskAppLogo
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.core.std.file.ext.fetchHook
import org.dweb_browser.sys.window.core.constant.LocalWindowMM
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.toStrict
import org.dweb_browser.sys.window.render.AppLogo

@Composable
fun JmmAppInstallManifest.IconRender(
  size: Dp = LocalTextStyle.current.fontSize.value.let { if (it.isNaN()) 32f else it * 2 }.dp,
) {
  val runtime = LocalWindowMM.current
  val isLocalApp = remember(id) { runtime.bootstrapContext.dns.query(id)?.mmid == id }
  val fetchHook = when {
    isLocalApp -> runtime.blobFetchHook
    else -> runtime.fetchHook
  }
  println("QAQ isLocalApp=$isLocalApp")
  when (val icon = remember(icons) { icons.toStrict().pickLargest() }) {
    null -> AppLogo.fromUrl(logo, fetchHook = fetchHook)
    else -> AppLogo.from(icon, fetchHook = fetchHook)
  }.toDeskAppLogo().Render(Modifier.size(size))
}
