package org.dweb_browser.sys.window.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.UUID
import org.dweb_browser.core.help.AdapterManager

typealias CreateWindowAdapter = suspend (winState: WindowState) -> WindowController?

data class WindowRenderScope(val width: Float, val height: Float, val scale: Float)
typealias WindowRenderProvider = @Composable WindowRenderScope.(modifier: Modifier) -> Unit

/**
 * 创建器窗口 的适配器管理
 */
class CreateWindowAdapterManager : AdapterManager<CreateWindowAdapter>() {
  val renderProviders = ChangeableMap<UUID, WindowRenderProvider>()

  @Composable
  fun rememberRender(wid: UUID): WindowRenderProvider? {
    var render by remember(wid) {
      mutableStateOf<WindowRenderProvider?>(@Composable {
        // TODO 显示配置的启动屏
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "加载中")
          }
        }
      });
    }
    DisposableEffect(wid) {
      val off = createWindowAdapterManager.renderProviders.onChange {
        render = it.origin[wid]
      }
      render = createWindowAdapterManager.renderProviders[wid]
      onDispose {
        off()
      }
    }
    return render
  }

  suspend fun createWindow(winState: WindowState): WindowController {
    for (adapter in adapters) {
      val winCtrl = adapter(winState)
      if (winCtrl != null) {
        /// 窗口创建成功，将窗口保存到实例集合中
        windowInstancesManager.add(winCtrl)
        return winCtrl;
      }
    }
    throw Exception("no support create native window, owner:${winState.constants.owner} provider:${winState.constants.provider}")
  }
}

val createWindowAdapterManager = CreateWindowAdapterManager();


