package org.dweb_browser.sys.window.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.AdapterManager
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.compose.MetaBallLoadingView
import org.dweb_browser.helper.defaultAsyncExceptionHandler
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.LocalWindowControllerTheme
import org.dweb_browser.sys.window.render.watchedState

typealias CreateWindowAdapter = suspend (winState: WindowState) -> WindowController?

data class WindowContentRenderScope(
  val widthDp: Dp,
  val heightDp: Dp,
  val scale: Float = 1f,
  val isResizing: Boolean = false,
) {
  val width get() = widthDp.value
  val height get() = heightDp.value
  override fun toString(): String {
    return "WindowContentRenderScope(width=$width, height=$height, scale=$scale, isResizing=$isResizing)"
  }

  fun copyFrom(boxConstraints: BoxWithConstraintsScope) = copy(
    widthDp = boxConstraints.maxWidth, heightDp = boxConstraints.maxHeight
  )

  companion object {
    val Unspecified = WindowContentRenderScope(Dp.Unspecified, Dp.Unspecified)
  }
}
typealias WindowRenderProvider = @Composable WindowContentRenderScope.(modifier: Modifier) -> Unit

/**
 * 窗口的适配器管理
 * 提供了窗口创建、窗口渲染的相关适配器功能
 */
class WindowAdapterManager : AdapterManager<CreateWindowAdapter>() {
  val renderProviders = ChangeableMap<String, WindowRenderProvider>()
  fun provideRender(rid: String, render: WindowRenderProvider): () -> Boolean {
    renderProviders[rid] = render
    return {
      renderProviders.remove(rid, render)
    }
  }

  @Composable
  fun rememberRender(rid: String): WindowRenderProvider? {
    var render by remember(rid) {
      mutableStateOf<WindowRenderProvider?>(@Composable {
        // TODO 显示配置的启动屏
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "加载中")
          }
        }
      });
    }
    DisposableEffect(rid) {
      val off = windowAdapterManager.renderProviders.onChange {
        render = it.origin[rid]
      }
      render = windowAdapterManager.renderProviders[rid]
      onDispose {
        off()
      }
    }
    return render
  }

  @Composable
  fun Renderer(
    rid: String, windowRenderScope: WindowContentRenderScope, contentModifier: Modifier = Modifier,
  ) {
    val colorScheme by LocalWindowController.current.watchedState { colorScheme }
    DwebBrowserAppTheme(colorScheme.toBooleanOrNull(isDark = true)) {
      when (val render = windowAdapterManager.rememberRender(rid)) {
        null -> {
          Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
          ) {
            /*Text(
              "Op！视图被销毁了",
              modifier = Modifier.align(Alignment.Center),
              style = typography.bodyMedium.copy(
                color = colorScheme.error,
              )
            )*/
            MetaBallLoadingView(modifier = Modifier.size(100.dp))
          }
        }

        else -> {
          val theme = LocalWindowControllerTheme.current
          CompositionLocalProvider(
            LocalContentColor provides theme.themeContentColor,
          ) {
            /**
             * 视图的宽高随着窗口的缩小而缩小，随着窗口的放大而放大，
             * 但这些缩放不是等比的，而是会以一定比例进行换算。
             */
            render(
              windowRenderScope,
              contentModifier,
            )
          }
        }
      }
    }
  }

  override fun append(order: Int, adapter: CreateWindowAdapter): () -> Boolean {
    return super.append(order, adapter)
  }

  private suspend fun CreateWindowAdapter.createAndSave(winState: WindowState): WindowController? {
    val winCtrl = this(winState)
    return if (winCtrl != null) {
      /// 窗口创建成功，将窗口保存到实例集合中
      windowInstancesManager.add(winCtrl)
      winCtrl;
    } else null
  }

  suspend fun createWindow(winState: WindowState): WindowController {
    for (adapter in adapters) {
      return adapter.createAndSave(winState) ?: continue
    }

    /// 等待3秒，期间如果有适配器提供进来，也能启动
    val waiter = CompletableDeferred<WindowController?>()
    onChange { adapter ->
      when (val win = adapter.createAndSave(winState)) {
        null -> {}
        else -> {
          offListener()
          waiter.complete(win)
        }
      }
    }
    // TODO
    CoroutineScope(defaultAsyncExceptionHandler).launch {
      delay(3000);
      waiter.complete(null)
    }
    return waiter.await()
      ?: throw Exception("no support create native window, owner:${winState.constants.owner} provider:${winState.constants.provider}")
  }
}

val windowAdapterManager = WindowAdapterManager();


