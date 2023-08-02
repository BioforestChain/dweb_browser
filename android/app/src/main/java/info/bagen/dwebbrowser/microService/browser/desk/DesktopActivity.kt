package info.bagen.dwebbrowser.microService.browser.desk

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import info.bagen.dwebbrowser.base.BaseActivity
import info.bagen.dwebbrowser.microService.core.WindowState
import info.bagen.dwebbrowser.microService.core.windowAdapterManager
import info.bagen.dwebbrowser.ui.theme.DwebBrowserAppTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


@SuppressLint("ModifierFactoryExtensionFunction")
fun WindowState.Rectangle.toModifier(modifier: Modifier = Modifier) = modifier
  .offset(left.dp, top.dp)
  .size(width.dp, height.dp)

class DesktopActivity : BaseActivity() {
  private var controller: DeskController? = null
  private fun bindController(sessionId: String?): DeskController {
    /// 解除上一个 controller的activity绑定
    controller?.activity = null

    return DesktopNMM.deskControllers[sessionId]?.also { desktopController ->
      desktopController.activity = this
      controller = desktopController
    } ?: throw Exception("no found controller by sessionId: $sessionId")
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val desktopController = bindController(intent.getStringExtra("deskSessionId"))
    val taskBarSessionId = intent.getStringExtra("taskBarSessionId")

    val context = this@DesktopActivity
    context.startActivity(Intent(context, TaskbarActivity::class.java).also {
      it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      it.putExtras(Bundle().also { b -> b.putString("taskBarSessionId", taskBarSessionId) })
    });

    /**
     * 窗口管理器
     */
    val desktopWindowsManager = DesktopWindowsManager(this)

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
              for (win in desktopWindowsManager.winList.value) {
                key(win.id) {
                  /// 渲染窗口
                  win.Render()
                }
              }
            }
          }
        }
      }
    }
  }
}

