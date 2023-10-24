package org.dweb_browser.sys.window.core

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.compose.rememberImageLoader
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.sys.window.core.constant.LocalWindowMM

@Serializable
sealed class ModalState() {
  abstract val callbackUrl: String?
  val modalId = randomUUID()

  /**
   * 关闭提示
   * 如果非空，那么在用户尝试主动关闭模态窗口的时候，会弹出报警提示
   */
  val closeTip: String? = null

  @Transient
  val isOpen = mutableStateOf(false)
//
//  @Transient
//  protected val onDismissSignal = SimpleSignal()
//
//  @Transient
//  val onDismiss = onDismissSignal.toListener()

  @Composable
  abstract fun Render()

  @Transient
  private var _renderId: String? = null

  var renderId
    get() = _renderId ?: (parent.id + "/" + modalId)
    protected set(value) {
      _renderId = value
    }

  @Transient
  private lateinit var parent: WindowController
  internal fun setParent(win: WindowController) {
    parent = win
    win.state.modals += modalId to this
  }


  fun sendCallback(mm: MicroModule, callbackData: ModalCallback) = callbackUrl?.also { url ->
    mm.ioAsyncScope.launch {
      mm.nativeFetch(
        PureRequest.fromJson(
          url, IpcMethod.POST, body = callbackData
        )
      )
    }
  }
}

interface IAlertModalArgs {
  val title: String
  val message: String
  val iconUrl: String?
  val iconAlt: String?
  val confirmText: String?
  val dismissText: String?
}

@Serializable
sealed class ModalCallback {
  abstract val sessionId: Int
}

@Serializable
@SerialName("open")
data class OpenModalCallback(override val sessionId: Int) : ModalCallback();
@Serializable
@SerialName("close")
data class CloseModalCallback(override val sessionId: Int) : ModalCallback();
@Serializable
@SerialName("close-alert")
data class CloseAlertModalCallback(override val sessionId: Int, val confirm: Boolean) :
  ModalCallback();

@Serializable
@SerialName("alert")
data class AlertModal internal constructor(
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
      iconUrl: String?,
      iconAlt: String?,
      confirmText: String?,
      dismissText: String?,
      callbackUrl: String?,
    ) = AlertModal(
      title,
      message,
      iconUrl,
      iconAlt,
      confirmText,
      dismissText,
      callbackUrl,
    ).also { it.setParent(this) }
  }

  @Composable
  override fun Render() {
    val mm = LocalWindowMM.current
    var show by isOpen
    var sessionAcc = remember { 0 }

    if (!show) {
      return
    }
    val sessionId = remember { sessionAcc++; sessionAcc }
    // 发送 open 的信号
    LaunchedEffect(sessionId) { sendCallback(mm, OpenModalCallback(sessionId)) }
    // alert的默认返回值
    var confirm = remember { false }

    AlertDialog(
      onDismissRequest = {
        show = false
        sendCallback(mm, CloseAlertModalCallback(sessionId, confirm))
      },
      confirmButton = {
        when (val text = confirmText) {
          null -> {}
          else -> ElevatedButton(onClick = { confirm = true; show = false }) {
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
  /// 这几个参数是未来使用FullScreenBottomSheets来实现，就是顶部有一条信息栏，将 bottom-sheets 拉上去的时候，融合信息栏，信息栏会变成 top-bar
  val title: String?
  val iconUrl: String?
  val iconAlt: String?
}

@Serializable
@SerialName("bottom-sheets")
class BottomSheetsModal private constructor(
  override val title: String? = null,
  override val iconUrl: String? = null,
  override val iconAlt: String? = null,
  override val callbackUrl: String? = null
) : ModalState(), IBottomSheetModal {
  companion object {
    fun WindowController.createBottomSheetsModal(
      title: String?, iconUrl: String?, iconAlt: String?, callbackUrl: String?
    ) = BottomSheetsModal(title, iconUrl, iconAlt, callbackUrl).also { it.setParent(this) }
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Render() {
    val mm = LocalWindowMM.current
    var show by isOpen
    var sessionAcc = remember { 0 }

    if (!show) {
      return
    }
    val sessionId = remember { sessionAcc++; sessionAcc }
    // 发送 open 的信号
    LaunchedEffect(sessionId) { sendCallback(mm, OpenModalCallback(sessionId)) }
    key(sessionId) {

      /// TODO 等1.5.10稳定版放出，我们就使用 ModalBottomSheet 组件来进行绘制，代码几乎不变
      ModalBottomSheet(onDismissRequest = {
        show = false;
        sendCallback(mm, CloseModalCallback(sessionId))
      }) {
        BoxWithConstraints {
          val windowRenderScope = remember(maxWidth, maxHeight) {
            WindowRenderScope.fromDp(maxWidth, maxHeight, 1f)
          }
          windowAdapterManager.Renderer(renderId, windowRenderScope)
        }
      }
    }

  }
}

