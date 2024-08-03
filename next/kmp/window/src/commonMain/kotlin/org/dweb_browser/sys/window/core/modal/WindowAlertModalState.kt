package org.dweb_browser.sys.window.core.modal


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.constant.LocalWindowMM

//#region Alert

interface IAlertModalArgs {
  val title: String
  val message: String
  val iconUrl: String?
  val iconAlt: String?
  val confirmText: String?
  val dismissText: String?
}

@Serializable
@SerialName("alert")
class AlertModalState private constructor(
  override val title: String,
  override val message: String,
  override val iconUrl: String? = null,
  override val iconAlt: String? = null,
  override val confirmText: String? = null,
  override val dismissText: String? = null,
  override val callbackUrl: String? = null,
) : ModalState(), IAlertModalArgs {
  companion object {
    fun WindowController.createAlertModal(
      title: String,
      message: String,
      iconUrl: String? = null,
      iconAlt: String? = null,
      confirmText: String? = null,
      dismissText: String? = null,
      callbackUrl: String? = null,
    ) = AlertModalState(
      title, message, iconUrl, iconAlt, confirmText, dismissText, callbackUrl,
    ).also { it.initParent(this) }
  }

  @OptIn(FlowPreview::class)
  @Composable
  override fun Render() {
    val mm = LocalWindowMM.current
    val show by isOpenState

    fun onModalDismissRequest(isDismiss: Boolean) = mm.scopeLaunch(cancelable = false) {
      dismissFlow.emit(isDismiss)
    }

    /// 渲染关闭提示
    TryShowCloseTip(onConfirmToClose = { onModalDismissRequest(true) })


    if (!show) {
      return
    }

    DisposableEffect(Unit) {
      // 发送 open 的信号
      sendCallback(mm, OpenModalCallback(sessionId))

      debugModal("DisposableEffect", " disposable")
      val job = mm.scopeLaunch(cancelable = true) {
        dismissFlow.debounce(200).collect {
          debugModal("dismissFlow", "close=$it")
          if (show && it) {
            safeClose(mm)
          }
        }
      }
      onDispose {
        job.cancel()
        /// 如果被销毁，那么也要进行安全的关闭
        /// 关闭动作只能被 dismiss 触发，不能因为Dispose触发，否则Activity重载时就会导致销毁
      }
    }
    // alert的默认返回值
    var confirm = remember { false }

    AlertDialog(
      onDismissRequest = {
        onModalDismissRequest(true)
        sendCallback(mm, CloseAlertModalCallback(sessionId, confirm))
        if (once) {
          mm.scopeLaunch(cancelable = false) {
            parent.removeModal(mm, modalId)
          }
        }
      },
      confirmButton = {
        when (val text = confirmText) {
          null -> {}
          else -> ElevatedButton(onClick = { confirm = true; onModalDismissRequest(true) }) {
            Text(text)
          }
        }
      },
      dismissButton = {
        when (val text = dismissText) {
          null -> {}
          else -> Button(onClick = { onModalDismissRequest(true) }) {
            Text(text)
          }
        }
      },
      icon = {
        when (val url = iconUrl) {
          null -> Icon(imageVector = Icons.Default.WarningAmber, contentDescription = iconAlt)
          else -> {
            /**
             * IconButtonTokens.IconSize
             */
            /**
             * IconButtonTokens.IconSize
             */
            val iconSize = 24.dp
            PureImageLoader.SmartLoad(url, iconSize, iconSize, hook = mm.blobFetchHook)
              .with(onError = {
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
//#endregion