package org.dweb_browser.browser.desk.render

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.launch
import org.dweb_browser.browser.desk.model.TaskbarAppModel
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.compose.pointerActions
import org.dweb_browser.helper.platform.theme.LocalColorful
import org.dweb_browser.sys.haptics.ext.vibrateHeavyClick
import org.dweb_browser.sys.haptics.ext.vibrateImpact
import org.dweb_browser.sys.window.render.AppIconContainer
import org.dweb_browser.sys.window.render.AppLogo

@Composable
internal fun TaskBarAppIcon(
  app: TaskbarAppModel,
  microModule: NativeMicroModule.NativeRuntime,
  openAppOrActivate: () -> Unit,
  quitApp: () -> Unit,
  toggleWindow: () -> Unit,
  containerAlpha: Float? = null,
  shadow: Dp? = null,
  popupOffset: IntOffset? = null,
  modifier: Modifier = Modifier,
) {

  val scope = rememberCoroutineScope()
  var showQuit by remember(app.isShowClose) { mutableStateOf(app.isShowClose) }

  var isHover by remember { mutableStateOf(false) }

  val scaleTargetValue = when {
    app.opening -> 0.9f
    isHover -> when {
      app.running -> 1.05f
      else -> 0.9f
    }

    else -> 1f
  }

  BoxWithConstraints(
    contentAlignment = Alignment.Center,
    modifier = modifier.scale(
      animateFloatAsState(
        scaleTargetValue, when {
          scaleTargetValue >= 1f -> spring(Spring.DampingRatioHighBouncy)
          else -> tween(200)
        }
      ).value
    ).pointerActions(
      onHoverStart = {
        scope.launch {
          microModule.vibrateImpact()
        }
        isHover = true
      },
      onHoverEnd = {
        isHover = false
      },
      onTap = {
        openAppOrActivate()
      },
      onDoubleTap = {
        scope.launch {
          microModule.vibrateImpact()
        }
        openAppOrActivate()
        if (app.running) {
          toggleWindow()
        }
      },
      onMenu = {
        if (app.running) {
          scope.launch {
            microModule.vibrateHeavyClick()
          }
          showQuit = !showQuit
        }
      },
    ),
  ) {
    AppLogo.from(app.icon, fetchHook = microModule.blobFetchHook).toDeskAppIcon(
      AppIconContainer(shadow = shadow ?: if (app.running) 2.dp else null),
      containerColor = if (app.running) null else LocalColorful.current.Gray.Shade_100,
      containerAlpha = containerAlpha ?: if (app.running) 1f else 0.8f,
    ).Render(
      logoModifier = if (showQuit) Modifier.blur(4.dp) else Modifier,
      innerContent = {
        if (showQuit) {
          Popup(
            onDismissRequest = {
              showQuit = false
            },
            offset = popupOffset ?: IntOffset.Zero
          ) {
            CloseButton(
              Color.Black,
              Modifier.requiredSize(size = maxWidth).clickableWithNoEffect {
                quitApp()
                showQuit = false
              },
            )
          }
        }
      },
    )
  }
}