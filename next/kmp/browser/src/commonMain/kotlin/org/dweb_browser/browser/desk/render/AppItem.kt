package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dweb_browser.browser.desk.model.DesktopAppModel
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.helper.compose.div
import org.dweb_browser.sys.window.render.AppLogo

@Composable
internal fun AppItem(
  app: DesktopAppModel,
  microModule: NativeMicroModule.NativeRuntime,
  modifier: Modifier,
) {
  val density = LocalDensity.current.density
  Column(
    modifier = modifier.padding(top = 12.dp, bottom = 8.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    AppLogo.from(app.icon, fetchHook = microModule.blobFetchHook).toDeskAppIcon()
      .Render(Modifier.size(52.dp).onGloballyPositioned {
        app.size = it.size / density
        app.offset = it.positionInWindow() / density
      }.jump(app.running == DesktopAppModel.DesktopAppRunStatus.Opening))
    Text(
      text = app.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = TextStyle(
        color = Color.White,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Thin,
        shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, 2f), 4f)
      ), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
  }
}