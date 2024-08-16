package org.dweb_browser.browser.desk.render

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.launch
import org.dweb_browser.browser.desk.model.TaskbarAppModel
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.platform.theme.LocalColorful
import org.dweb_browser.sys.window.render.AppIconContainer
import org.dweb_browser.sys.window.render.AppLogo

@Composable
internal fun TaskBarAppIcon(
  app: TaskbarAppModel,
  microModule: NativeMicroModule.NativeRuntime,
  padding: Dp,
  openApp: () -> Unit,
  quitApp: () -> Unit,
  toggleWindow: () -> Unit,
  modifier: Modifier = Modifier,
) {

  val scaleValue = remember { Animatable(1f) }
  val scope = rememberCoroutineScope()
  var showQuit by remember(app.isShowClose) { mutableStateOf(app.isShowClose) }
  fun doHaptics() {
    scope.launch {
      microModule.nativeFetch("file://haptics.sys.dweb/vibrateHeavyClick")
    }
  }

  BoxWithConstraints(
    contentAlignment = Alignment.Center,
    modifier = modifier.graphicsLayer {
      scaleX = scaleValue.value
      scaleY = scaleValue.value
    }.padding(start = padding, top = padding, end = padding)
      .desktopAppItemActions(
        onHoverStart = {
          scope.launch {
            scaleValue.animateTo(1.05f)
          }
        },
        onHoverEnd = {
          scope.launch {
            scaleValue.animateTo(1.0f)
          }
        },
        onOpenApp = {
          if (!app.running) {
            openApp()
          }
        },
        onDoubleTap = {
          if (app.running) {
            toggleWindow()
          } else {
            openApp()
          }
        },
        onOpenAppMenu = {
          if (app.running) {
            doHaptics()
            showQuit = !showQuit
          }
        },
      ),
  ) {
    AppLogo.from(app.icon, fetchHook = microModule.blobFetchHook)
      .toDeskAppIcon(
        AppIconContainer(shadow = if (app.running) 2.dp else null),
        containerColor = if (app.running) null else LocalColorful.current.Gray.Shade_100,
        containerAlpha = if (app.running) 1f else 0.8f,
      ).Render(
        logoModifier = if (showQuit) Modifier.blur(4.dp) else Modifier,
        innerContent = {
          if (showQuit) {
            Popup(
              onDismissRequest = {
                showQuit = false
              },
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