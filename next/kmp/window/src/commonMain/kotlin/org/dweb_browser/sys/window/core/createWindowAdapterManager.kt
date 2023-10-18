package org.dweb_browser.sys.window.core

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import org.dweb_browser.core.help.AdapterManager
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.sys.window.render.LocalWindowControllerTheme

typealias CreateWindowAdapter = suspend (winState: WindowState) -> WindowController?

data class WindowRenderScope internal constructor(
  val width: Float,
  val height: Float,
  val scale: Float,
  val widthDp: Dp,
  val heightDp: Dp,
) {
  companion object {
    fun fromDp(
      widthDp: Dp,
      heightDp: Dp,
      scale: Float,
    ) = WindowRenderScope(widthDp.value, heightDp.value, scale, widthDp, heightDp)
  }

  constructor(
    width: Float,
    height: Float,
    scale: Float,
  ) : this(width, height, scale, width.dp, height.dp)
}
typealias WindowRenderProvider = @Composable WindowRenderScope.(modifier: Modifier) -> Unit

/**
 * 创建器窗口 的适配器管理
 */
class CreateWindowAdapterManager : AdapterManager<CreateWindowAdapter>() {
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
      val off = createWindowAdapterManager.renderProviders.onChange {
        render = it.origin[rid]
      }
      render = createWindowAdapterManager.renderProviders[rid]
      onDispose {
        off()
      }
    }
    return render
  }

  @Composable
  fun Renderer(
    rid: String,
    windowRenderScope: WindowRenderScope,
    @SuppressLint("ModifierParameter") contentModifier: Modifier = Modifier
  ) {
    when (val render = createWindowAdapterManager.rememberRender(rid)) {
      null -> {
        val colorScheme = MaterialTheme.colorScheme;
        val typography = MaterialTheme.typography;
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.errorContainer)
        ) {
          Text(
            "Op！视图被销毁了",
            modifier = Modifier.align(Alignment.Center),
            style = typography.bodyMedium.copy(
              color = colorScheme.error,
            )
          )
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
            Modifier
              .requiredSize(windowRenderScope.width.dp, windowRenderScope.height.dp)
              .then(contentModifier),
          )
        }
      }
    }
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


