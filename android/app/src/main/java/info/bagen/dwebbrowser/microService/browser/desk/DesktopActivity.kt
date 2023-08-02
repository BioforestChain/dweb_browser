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


@SuppressLint("ModifierFactoryExtensionFunction")
fun WindowState.Rectangle.toModifier(modifier: Modifier = Modifier) = modifier
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

  private val winList = mutableStateListOf<DesktopWindowController>()
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val desktopController = bindController(intent.getStringExtra("sessionId"))

    /// 创建成功，提供适配器来渲染窗口
    val offAdapter = windowAdapterManager.append { winState ->
      with(winState.bounds) {
        left = 150f
        top = 250f
        width = 200f
        height = 300f
      }
      val winCtrl = DesktopWindowController(this, winState)
        .also { winCtrl ->
          winCtrl.onDestroy {
            winList.remove(winCtrl)
          }
        }
      winList.add(winCtrl)
      winCtrl
    }
    /// Activity销毁的时候，移除窗口
    onDestroyActivity { offAdapter(Unit) }


    val context = this@DesktopActivity
    context.startActivity(Intent(context, TaskbarActivity::class.java).also {
      it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    });

    /// 上一个聚焦的窗口对象
    var preFocusedWin: DesktopWindowController? = null;

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
                key(win.id) {
                  LaunchedEffect(win) {
                    /// 如果有一个新的窗口聚焦了，那么上一个聚焦的窗口释放聚焦
                    win.onFocus {
                      preFocusedWin?.blur()
                      preFocusedWin = win;
                      Unit
                    }
                    /// 如果窗口释放聚焦，那么释放 preFocusedWin 的引用
                    win.onBlur {
                      if (preFocusedWin == win) {
                        preFocusedWin = null
                      }
                    }
                    /// 第一次装载窗口，默认将它聚焦
                    win.focus()
                  }
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

