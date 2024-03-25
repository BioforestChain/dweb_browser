package org.dweb_browser.sys.window.core.modal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.constant.LocalWindowMM
import org.dweb_browser.sys.window.core.constant.LowLevelWindowAPI
import org.dweb_browser.sys.window.render.IconRender
import org.dweb_browser.sys.window.render.IdRender
import org.dweb_browser.sys.window.render.LocalWindowControllerTheme


//#region BottomSheet

interface IBottomSheetModal {
  /// 这几个参数是未来使用FullScreenBottomSheets来实现，就是顶部有一条信息栏，将 bottom-sheets 拉上去的时候，融合信息栏，信息栏会变成 top-bar
  val title: String?
  val iconUrl: String?
  val iconAlt: String?
}


@Serializable
@SerialName("bottom-sheets")
class BottomSheetsModalState private constructor(
  override val title: String? = null,
  override val iconUrl: String? = null,
  override val iconAlt: String? = null,
  override val callbackUrl: String? = null,
) : ModalState(), IBottomSheetModal {
  companion object {
    fun WindowController.createBottomSheetsModal(
      title: String? = null,
      iconUrl: String? = null,
      iconAlt: String? = null,
      callbackUrl: String? = null,
    ) = BottomSheetsModalState(title, iconUrl, iconAlt, callbackUrl).also {
      it.initParent(this);
    }
  }

  /**
   * 是否已经展开，用于确保展开后才能进行 hidden 收回
   */
  @Transient
  internal var hasExpanded = false

  @LowLevelWindowAPI
  override fun close() {
    super.close()
    this.hasExpanded = false
  }

  @OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
  @Composable
  override fun Render() {
    val mm = LocalWindowMM.current
    val show by isOpenState

    fun emitDismiss(isDismiss: Boolean) {
      mm.ioAsyncScope.launch {
        dismissFlow.emit(isDismiss)
      }
    }

    /**
     * 传入 EmitModalVisibilityState 指令，如果指令让状态发生了改变，那么返回 成功:true
     */
    val emitModalVisibilityChange: (state: EmitModalVisibilityState) -> Boolean = { state ->
      when (state) {
        EmitModalVisibilityState.Open -> {
          emitDismiss(false)
          true
        }

        EmitModalVisibilityState.TryClose -> closeTip.let { closeTip ->
          if (closeTip.isNullOrEmpty() || isShowCloseTip) {
            emitDismiss(true)
            hasExpanded
          } else {
            showCloseTip.value = closeTip
            false
          }
        }

        EmitModalVisibilityState.ForceClose -> {
          emitDismiss(true);
          false
        }
      }
    }

    /// 渲染关闭提示
    TryShowCloseTip(onConfirmToClose = { emitModalVisibilityChange(EmitModalVisibilityState.ForceClose) })

    if (!show) {
      return
    }

    DisposableEffect(Unit) {
      // 发送 open 的信号
      sendCallback(mm, OpenModalCallback(sessionId))

      debugModal("DisposableEffect", " disposable")
      val job = dismissFlow.map { dismiss ->
        if (!dismiss) {
          hasExpanded = true
        }
        dismiss
      }.debounce(200).map { dismiss ->
        debugModal("dismissFlow", "close=$dismiss hasExpanded=$hasExpanded")
        if (dismiss && show && hasExpanded) {
          safeClose(mm)
        }
      }.launchIn(mm.ioAsyncScope)
      onDispose {
        job.cancel()
        /// 关闭动作只能被 dismiss 触发，不能因为Dispose触发，否则Activity重载时就会导致销毁
      }
    }
    RenderImpl(emitModalVisibilityChange)
  }
}

enum class EmitModalVisibilityState {
  Open, TryClose, ForceClose,
}

@Composable
internal expect fun BottomSheetsModalState.RenderImpl(emitModalVisibilityChange: (state: EmitModalVisibilityState) -> Boolean)

@Composable
fun BottomSheetsModalState.TitleBarWithCustomCloseBottom(
  closeBottom: @Composable (Modifier) -> Unit, content: @Composable BoxScope.() -> Unit
) {
  val winTheme = LocalWindowControllerTheme.current
  val contentColor = winTheme.topContentColor
  val win = parent
  val size = 48.dp
  Box(
    modifier = Modifier.height(size).fillMaxSize(), Alignment.Center
  ) {
    /// 应用图标
    Box(
      modifier = Modifier.align(Alignment.CenterStart).size(size)
        .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
      contentAlignment = Alignment.CenterStart
    ) {
      win.IconRender(
        modifier = Modifier.fillMaxSize(), primaryColor = contentColor
      )
    }
    /// 应用身份 与 关闭按钮
    Row(
      Modifier.align(Alignment.BottomEnd),
      verticalAlignment = Alignment.Bottom,
    ) {
      win.IdRender(
        Modifier.height(22.dp).padding(vertical = 2.dp).alpha(0.4f), contentColor = contentColor
      )
      closeBottom(Modifier.size(size))
    }
    content()
  }
}

@Composable
fun BottomSheetsModalState.TitleBarWithOnClose(
  onClose: () -> Unit, content: @Composable BoxScope.() -> Unit
) {
  TitleBarWithCustomCloseBottom(
    { modifier ->
      val winTheme = LocalWindowControllerTheme.current
      Box(
        modifier.padding(13.dp), contentAlignment = Alignment.Center
      ) {
        IconButton(
          {
            onClose()
          }, colors = IconButtonDefaults.iconButtonColors(
            contentColor = winTheme.topBackgroundColor, containerColor = winTheme.topContentColor
          )
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Close Bottom Sheet",
            modifier = Modifier.padding(3.dp)
          )
        }
      }
    }, content
  )
}
//#endregion
