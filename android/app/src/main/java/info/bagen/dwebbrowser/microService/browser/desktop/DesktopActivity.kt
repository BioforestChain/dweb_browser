package info.bagen.dwebbrowser.microService.browser.desktop

import android.graphics.RuntimeShader
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Paint
import androidx.core.view.WindowCompat
import com.google.accompanist.web.WebView
import info.bagen.dwebbrowser.base.BaseActivity
import info.bagen.dwebbrowser.microService.browser.desktop.model.LocalDesktopViewItem
import info.bagen.dwebbrowser.microService.browser.desktop.model.LocalInstallList
import info.bagen.dwebbrowser.microService.browser.desktop.model.LocalOpenList
import info.bagen.dwebbrowser.microService.browser.desktop.ui.DesktopMainView
import info.bagen.dwebbrowser.ui.theme.DwebBrowserAppTheme
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.base.DWebViewItem

class DesktopActivity : BaseActivity() {
//  private val COLOR_SHADER_SRC =
//    """half4 main(float2 fragCoord) {
//      return half4(1,0,0,1);
//   }"""
//  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
//  val fixedColorShader = RuntimeShader(COLOR_SHADER_SRC)
//  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
//  val paint = Paint().also {
//    it.shader = fixedColorShader
//  }
//override fun onDrawForeground(canvas: Canvas?) {
//   canvas?.let {
//      fixedColorShader.setFloatUniform("iResolution", width.toFloat(), height.toFloat())
//      canvas.drawPaint(paint)
//   }
//}

  private var controller: DesktopController? = null
  private fun upsetRemoteMmid() {
    controller?.activity = null

    controller = DesktopNMM.desktopController?.also { desktopController ->
      desktopController.activity = this
    } ?: throw Exception("no found controller")
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    upsetRemoteMmid()

    setContent {
      DwebBrowserAppTheme {
        val scope = rememberCoroutineScope()
        controller?.let { desktopController ->
          desktopController.effect(activity = this@DesktopActivity)
          CompositionLocalProvider(
            LocalInstallList provides DesktopNMM.getInstallAppList(),
            LocalOpenList provides DesktopNMM.getRunningAppList(),
            LocalDesktopViewItem provides desktopController.createMainDwebView(),
          ) {
            DesktopMainView(
              viewItem = LocalDesktopViewItem.current
            ) { metaData ->
              scope.launch {
                desktopController.openApp(metaData)
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
    state = viewItem.state,
    modifier = Modifier.fillMaxSize()
  ) {
    viewItem.webView
  }
}