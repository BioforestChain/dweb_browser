package org.dweb_browser.browser.desk.render

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.launch
import org.dweb_browser.browser.desk.model.TaskbarAppModel
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.compose.clickableWithNoEffect

@Composable
internal fun TaskBarAppIcon(
  modifier: Modifier,
  app: TaskbarAppModel,
  microModule: NativeMicroModule.NativeRuntime,
  openApp: (mmid: String) -> Unit,
  quitApp: (mmid: String) -> Unit,
  toggleWindow: (mmid: String) -> Unit,
) {

  val scaleValue = remember { Animatable(1f) }
  val scope = rememberCoroutineScope()
  var showQuit by remember(app.isShowClose) { mutableStateOf(app.isShowClose) }

  fun doAnimation() {
    scope.launch {
      scaleValue.animateTo(1.1f)
      scaleValue.animateTo(1.0f)
    }
  }

  fun doHaptics() {
    scope.launch {
      microModule.nativeFetch("file://haptics.sys.dweb/vibrateHeavyClick")
    }
  }

  BoxWithConstraints(contentAlignment = Alignment.Center, modifier = modifier.graphicsLayer {
    scaleX = scaleValue.value
    scaleY = scaleValue.value
  }.padding(start = paddingValue.dp, top = paddingValue.dp, end = paddingValue.dp)
    .aspectRatio(1.0f)
    .pointerInput(app) {
      detectTapGestures(onPress = {
        doAnimation()
      }, onTap = {
        openApp(app.mmid)
      }, onDoubleTap = {
        if (app.running) {
          toggleWindow(app.mmid)
        } else {
          openApp(app.mmid)
        }
      }, onLongPress = {
        doHaptics()
        if (app.running) {
          showQuit = true
        } else {
          openApp(app.mmid)
        }
      })
    }) {

    key(app.icon) {
      BoxWithConstraints(Modifier.blur(if (showQuit) 1.dp else 0.dp)) {
        DeskCacheIcon(app.icon, microModule, maxWidth, maxHeight)
      }
    }

    if (showQuit) {
      if (taskBarCloseButtonUsePopUp()) {
        Popup(onDismissRequest = {
          showQuit = false
        }) {
          CloseButton(Modifier.size(maxWidth).clickableWithNoEffect {
            quitApp(app.mmid)
            showQuit = false
          })
        }
      } else {
        CloseButton(Modifier.size(maxWidth).clickableWithNoEffect {
          quitApp(app.mmid)
          showQuit = false
        })
      }
    }
  }
}