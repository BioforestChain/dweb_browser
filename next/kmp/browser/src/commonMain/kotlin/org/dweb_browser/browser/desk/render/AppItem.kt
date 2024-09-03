package org.dweb_browser.browser.desk.render

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
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
  edit: Boolean,
  editDragging: Boolean,
  microModule: NativeMicroModule.NativeRuntime,
  modifier: Modifier,
  iconModifier: Modifier,
) {
  val density = LocalDensity.current.density

  val shakeAnimation by rememberInfiniteTransition().animateFloat(
    -5f,
    5f,
    animationSpec = infiniteRepeatable(
      animation = tween(200, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    )
  )

  val scaleAnimation = animateFloatAsState(
    if (editDragging) 1.1f else 1.0f,
    spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
  )

  Column(
    modifier = modifier.fillMaxSize().padding(top = 8.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    AppLogo.from(app.icon, fetchHook = microModule.blobFetchHook).toDeskAppIcon()
      .Render(iconModifier.weight(0.65f).size(52.dp).graphicsLayer {
        if (editDragging) {
          scaleX = scaleAnimation.value
          scaleY = scaleAnimation.value
        } else if (edit) {
          rotationZ = shakeAnimation
        }
      }.onGloballyPositioned {
        app.size = it.size / density
        app.offset = it.positionInWindow() / density
      }.jump(app.running == DesktopAppModel.DesktopAppRunStatus.Opening))

    Text(
      text = app.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = TextStyle(
        color = Color.White,
        fontSize = 10.sp,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Light,
        shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, 2f), 4f)
      ), modifier = Modifier.weight(0.35f).fillMaxWidth().padding(top = 6.dp)
    )
  }
}