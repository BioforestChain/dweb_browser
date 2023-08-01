package info.bagen.dwebbrowser.microService.browser.desk

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import info.bagen.dwebbrowser.base.BaseActivity
import info.bagen.dwebbrowser.microService.core.WindowController
import info.bagen.dwebbrowser.microService.core.WindowState
import info.bagen.dwebbrowser.microService.core.windowAdapterManager
import info.bagen.dwebbrowser.ui.theme.DwebBrowserAppTheme
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.base.DWebViewItem


class DeskWindowController(
  override val androidContext: Context, private val winState: WindowState
) : WindowController() {
  override fun toJson() = winState

  @Composable
  fun Render() {
    Box(
      modifier = winState.bounds
        .modifier(Modifier)
        .shadow(2.dp)
    ) {
      Column {
        Box(
          modifier = Modifier
            .background(MaterialTheme.colorScheme.onPrimaryContainer)
            .fillMaxWidth()
        ) {
          Text(
            text = winState.title,
            style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onPrimary)
          )
        }
        Box(modifier = Modifier.fillMaxSize()) {
          windowAdapterManager.providers[winState.wid]?.also { it(Modifier.fillMaxSize()) } ?: Text(
            "Op！视图被销毁了",
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.headlineLarge.copy(
              color = MaterialTheme.colorScheme.error,
              background = MaterialTheme.colorScheme.errorContainer
            )
          )
        }
      }
    }
  }
}

@SuppressLint("ModifierFactoryExtensionFunction")
fun WindowState.Rectangle.modifier(modifier: Modifier = Modifier) =
  modifier
    .offset(left.dp, top.dp)
    .size(width.dp, height.dp)

class DesktopActivity : BaseActivity() {
  private var controller: DeskController? = null
  private fun bindController(sessionId: String?): DeskController {
    /// 解除上一个 controller的activity绑定
    controller?.activity = null

    return DesktopNMM.controllers[sessionId]?.also { desktopController ->
      desktopController.activity = this
      controller = desktopController
    } ?: throw Exception("no found controller by sessionId: $sessionId")
  }

  private val winList = mutableListOf<DeskWindowController>()
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val desktopController = bindController(intent.getStringExtra("sessionId"))


    /// 创建成功，提供适配器来渲染窗口
    val offAdapter = windowAdapterManager.append { winState ->
      with(winState.bounds) {
        left = 150
        top = 250
        width = 200
        height = 300
      }
      val winCtrl = DeskWindowController(this, winState)
      winList.add(winCtrl)
      winCtrl
    }
    /// Activity销毁的时候，移除窗口
    onDestroyActivity { offAdapter(Unit) }


    val context = this@DesktopActivity
    context.startActivity(Intent(context, TaskbarActivity::class.java).also {
      it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    });

    setContent {
      DwebBrowserAppTheme {
        val scope = rememberCoroutineScope()
        desktopController.effect(activity = this@DesktopActivity)

        CompositionLocalProvider(
          LocalInstallList provides desktopController.getInstallApps(),
          LocalOpenList provides desktopController.getOpenApps(),
          LocalDesktopView provides desktopController.createMainDwebView(),
        ) {
          Box {
            /// 桌面视图
            val desktopView = LocalDesktopView.current
            WebView(
              state = rememberWebViewState(url = desktopController.getDesktopUrl().toString()),
              modifier = Modifier.fillMaxSize(),
            ) {
              desktopView
            }
            /// 窗口视图
            Box {
              for (win in winList) {
                win.Render()
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun DesktopMainWebView(viewItem: DWebViewItem) {
  WebView(
    state = viewItem.state, modifier = Modifier.fillMaxSize()
  ) {
    viewItem.webView
  }
}

