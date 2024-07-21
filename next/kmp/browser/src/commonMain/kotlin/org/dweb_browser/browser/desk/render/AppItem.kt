package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import org.dweb_browser.browser.desk.model.DesktopAppModel
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.compose.div

@Composable
internal fun AppItem(
  modifier: Modifier,
  app: DesktopAppModel,
  microModule: NativeMicroModule.NativeRuntime,
) {
  val density = LocalDensity.current.density
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    DeskAppIcon(app, microModule, modifier = Modifier.onGloballyPositioned {
      app.size = it.size / density
      app.offset = it.positionInWindow()
    }.jump(app.running == DesktopAppModel.DesktopAppRunStatus.TORUNNING))
    Text(
      text = app.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = TextStyle(
        color = Color.White,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        shadow = Shadow(Color.Black, Offset(4f, 4f), 4f)
      ), modifier = Modifier.fillMaxWidth()
    )
  }
}