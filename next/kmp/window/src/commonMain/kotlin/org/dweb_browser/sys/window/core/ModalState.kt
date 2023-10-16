package org.dweb_browser.sys.window.core

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
//import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.compose.rememberImageLoader
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.sys.window.core.constant.LocalWindowMM
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.LocalWindowControllerTheme

///**
// * 模态层管理
// */
//class ModalsLayer(internal val viewController: PlatformViewController) {
//  val modalList = mutableStateListOf<ModalController>()
//}

//@Composable
//fun ModalsLayer.Render() {
//  val modal = modalList.firstOrNull()
//  if (modal != null) {
//    modal.state.Render()
//  }
//}

@Serializable
sealed class ModalState() {
  val modalId = randomUUID()

  @Transient
  val isOpen = mutableStateOf(false)

  @Transient
  protected val onDismissSignal = SimpleSignal()

  @Transient
  val onDismiss = onDismissSignal.toListener()

  @Composable
  abstract fun Render()
}

interface IAlertModal {
  val title: String
  val message: String
  val iconUrl: String?
  val iconAlt: String?
  val confirmText: String?
  val dismissText: String?
  val confirmCallbackUrl: String?
  val dismissCallbackUrl: String?
}

@Serializable
@SerialName("alert")
class AlertModal(
  override val title: String,
  override val message: String,
  override val iconUrl: String? = null,
  override val iconAlt: String? = null,
  override val confirmText: String? = null,
  override val dismissText: String? = null,
  override val confirmCallbackUrl: String? = null,
  override val dismissCallbackUrl: String? = null,
) : ModalState(), IAlertModal {

  @Composable
  override fun Render() {
    val mm = LocalWindowMM.current
    val win = LocalWindowController.current
    var show by isOpen

    if (!show) {
      return
    }
    AlertDialog(
      onDismissRequest = {
        show = false
        mm.ioAsyncScope.launch {
          onDismissSignal.emitAndClear()
          dismissCallbackUrl?.also { url ->
            mm.nativeFetch(url)
          }
        }
      },
      confirmButton = {
        when (val text = confirmText) {
          null -> {}
          else -> ElevatedButton(onClick = {
            show = false
            confirmCallbackUrl?.also { url ->
              mm.ioAsyncScope.launch {
                mm.nativeFetch(url)
              }
            }
          }) {
            Text(text)
          }
        }
      },
      dismissButton = {
        when (val text = dismissText) {
          null -> {}
          else -> Button(onClick = { show = false }) {
            Text(text)
          }
        }
      },
      icon = {
        when (val url = iconUrl) {
          null -> Icon(imageVector = Icons.Default.WarningAmber, contentDescription = iconAlt)
          else -> {
            val imageLoader = rememberImageLoader()

            /**
             * IconButtonTokens.IconSize
             */
            val iconSize = 24.dp
            imageLoader.load(url, iconSize, iconSize).with(onError = {
              Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = iconAlt)
            }, onBusy = {
              CircularProgressIndicator(
                modifier = Modifier.width(iconSize),
                color = MaterialTheme.colorScheme.surfaceVariant,
                trackColor = MaterialTheme.colorScheme.secondary,
              )
            }) {
              Image(bitmap = it, contentDescription = iconAlt)
            }
          }
        }
      },
      title = {
        Text(text = title)
      },
      text = {
        Text(text = message)
      },
    )
  }
}

interface IBottomSheetModal {
  val dismissCallbackUrl: String?
}

@Serializable
@SerialName("bottom-sheet")
class BottomSheetModal(override val dismissCallbackUrl: String?) :
  ModalState(), IBottomSheetModal {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Render() {
    val mm = LocalWindowMM.current
    val win = LocalWindowController.current
    var show by isOpen
    if (!show) {
      return
    }
    val render = createWindowAdapterManager.rememberRender(win.id + "/" + modalId);
//    val sheetState = rememberModalBottomSheetState()
//    ModalBottomSheet(onDismissRequest = {
//      show = false;
//      mm.ioAsyncScope.launch {
//        onDismissSignal.emitAndClear()
//        dismissCallbackUrl?.also { url ->
//          mm.nativeFetch(url)
//        }
//      }
//    }, sheetState = sheetState) {
//      val theme = LocalWindowControllerTheme.current
//      /**
//       * 视图的宽高随着窗口的缩小而缩小，随着窗口的放大而放大，
//       * 但这些缩放不是等比的，而是会以一定比例进行换算。
//       */
//      render?.also {
//        CompositionLocalProvider(
//          LocalContentColor provides theme.themeContentColor,
//        ) {
//          BoxWithConstraints {
//            it.invoke(
//              WindowRenderScope(
//                width = maxWidth.value,
//                height = maxHeight.value,
//                scale = 1f,
//              ),
//              Modifier.requiredSize(maxWidth, maxHeight),
//            )
//          }
//        }
//
//      } ?: Text(
//        "Op！视图被销毁了",
//        modifier = Modifier.align(Alignment.CenterHorizontally),
//        style = MaterialTheme.typography.bodyMedium.copy(
//          color = MaterialTheme.colorScheme.error,
//        )
//      )
//    }

  }
}

//
//class ModalController(val manager: ModalsLayer, val state: ModalState) {
//  init {
//    if (manager.modalList.add(this)) {
//      state.onDismiss {
//        manager.modalList.remove(this@ModalController)
//      }
//    }
//  }
//}
