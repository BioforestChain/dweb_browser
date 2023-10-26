package org.dweb_browser.sys.window.core

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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
import org.dweb_browser.sys.window.render.IconRender
import org.dweb_browser.sys.window.render.IdRender
import org.dweb_browser.sys.window.render.LocalWindowControllerTheme
import org.dweb_browser.sys.window.render.LocalWindowPadding

@Serializable
sealed class ModalState() {
  abstract val callbackUrl: String?
  val modalId = randomUUID()
  val once = false

  /**
   * 关闭提示
   * 如果非空，那么在用户尝试主动关闭模态窗口的时候，会弹出报警提示
   */
  val closeTip: String? = null

  @Transient
  val isOpen = mutableStateOf(false)

  @Transient
  val showCloseTip = mutableStateOf("")

  val isShowCloseTip get() = showCloseTip.value.isNotEmpty()

  @Composable
  abstract fun Render()

  @SerialName("renderId")
  private var _renderId: String = ""

  val renderId get() = _renderId

  @Transient
  protected lateinit var parent: WindowController
  internal fun setParent(win: WindowController) {
    parent = win
    win.state.modals += modalId to this
    _renderId = win.id + "/" + modalId
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

  @Composable
  protected fun RenderCloseTip(onConfirmToClose: suspend CoroutineScope.() -> Unit) {
    if (isShowCloseTip) {
      val scope = rememberCoroutineScope()
      AlertDialog(
        onDismissRequest = {
          showCloseTip.value = ""
        },
        title = { Text(text = "是否关闭抽屉面板") },
        text = { Text(text = showCloseTip.value) },
        confirmButton = {
          ElevatedButton(onClick = { showCloseTip.value = "" }) {
            Text("留下")
          }
        },
        dismissButton = {
          Button(onClick = {
            showCloseTip.value = "";
            scope.launch(block = onConfirmToClose)
          }) {
            Text("关闭")
          }
        },
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
      iconUrl: String? = null,
      iconAlt: String? = null,
      confirmText: String? = null,
      dismissText: String? = null,
      callbackUrl: String? = null,
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
        if (once) {
          mm.ioAsyncScope.launch {
            parent.removeModal(modalId)
          }
        }
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
      title: String? = null,
      iconUrl: String? = null,
      iconAlt: String? = null,
      callbackUrl: String? = null
    ) = BottomSheetsModal(title, iconUrl, iconAlt, callbackUrl).also { it.setParent(this) }
  }

  @OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
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

    /// TODO 等1.5.10稳定版放出，IOS才能使用这个组件

    val onModalDismissRequestFlow = remember {
      MutableSharedFlow<Boolean>()
    }
    println("SSSZ start")
    var hasStart = remember {
      false
    }

    DisposableEffect(onModalDismissRequestFlow) {
      println("SSSZ disposable")
      val job = onModalDismissRequestFlow.map {
        if (!it) {
          hasStart = true
        }
        it
      }.debounce(200).map {
        println("SSSR hide=$it opend=$hasStart")
        if (show && it && hasStart) {
          show = false;
          sendCallback(mm, CloseModalCallback(sessionId))
          if (once) {
            mm.ioAsyncScope.launch {
              parent.removeModal(modalId)
            }
          }
        }
      }.launchIn(mm.ioAsyncScope)
      onDispose {
        job.cancel()
      }
    }
    fun onModalDismissRequest(isDismiss: Boolean) = mm.ioAsyncScope.launch {
      onModalDismissRequestFlow.emit(isDismiss)
    }

    val sheetState = rememberModalBottomSheetState(confirmValueChange = {
      println("SSS $it")
      when (it) {
        SheetValue.Hidden -> {
          if (closeTip.isNullOrEmpty() || isShowCloseTip) {
            onModalDismissRequest(true)
            hasStart
          } else {
            showCloseTip.value = closeTip
            false
          }
        }

        SheetValue.Expanded -> {
          onModalDismissRequest(false);
          true
        }

        SheetValue.PartiallyExpanded -> {
          onModalDismissRequest(false);
          true
        }
      }
    });

    val density = LocalDensity.current
    val defaultWindowInsets = BottomSheetDefaults.windowInsets
    val modalWindowInsets = remember(defaultWindowInsets) {
      defaultWindowInsets.only(WindowInsetsSides.Top)
    }

    val win = parent;
    val winPadding = LocalWindowPadding.current
    val winTheme = LocalWindowControllerTheme.current
    val contentColor = winTheme.topContentColor
    ModalBottomSheet(sheetState = sheetState, dragHandle = {
      Box(
        modifier = Modifier
          .height(48.dp)
          .fillMaxSize()
          .padding(horizontal = 14.dp),
        Alignment.Center
      ) {
        BottomSheetDefaults.DragHandle()
        /// 应用图标
        Box(
          modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart
        ) {
          win.IconRender(
            modifier = Modifier.size(28.dp), primaryColor = contentColor
          )
        }
        /// 应用身份
        win.IdRender(
          Modifier
            .align(Alignment.BottomEnd)
            .height(22.dp)
            .padding(vertical = 2.dp)
            .alpha(0.4f),
          contentColor = contentColor
        )
      }
    }, windowInsets = modalWindowInsets, onDismissRequest = { onModalDismissRequest(true) }) {
      /// 显示内容
      BoxWithConstraints(
        Modifier.padding(
          start = winPadding.left.dp,
          end = winPadding.right.dp,
          bottom = (defaultWindowInsets.getBottom(density) / density.density).dp
        )
      ) {
        val windowRenderScope = remember(winPadding) {
          WindowRenderScope.fromDp(maxWidth, maxHeight, 1f)
        }
        windowAdapterManager.Renderer(
          renderId,
          windowRenderScope,
          Modifier.clip(winPadding.contentRounded.toRoundedCornerShape())
        )
      }

    }

    RenderCloseTip(onConfirmToClose = { sheetState.hide() })
  }
}

