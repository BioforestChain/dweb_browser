import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.LaunchedEffect
import com.teamdev.jxbrowser.browser.event.BrowserClosed
import com.teamdev.jxbrowser.browser.event.TitleChanged
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.dwebview.IDWebView.Companion.registryDevtoolsTray
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.helper.globalEmptyScope
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.webViewEngine
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.image.compose.rememberOffscreenWebCanvas
import org.jetbrains.compose.reload.HotReload
import kotlin.system.exitProcess

suspend fun main(vararg args: String) {
  /// 桌面端强制启用新版桌面, 要在最前面开启，因为Windows平台需要开启compose.swing.render.on.graphics
  envSwitch.enable(ENV_SWITCH_KEY.DESKTOP_STYLE_COMPOSE)

  // 先预先加载jxbrowser engine，避免第一次加载webview时需要解压jxbrowser导致启动慢
  if (envSwitch.isEnabled(ENV_SWITCH_KEY.DESKTOP_STYLE_COMPOSE)) {
    globalEmptyScope.launch {
      println("QWQ chromiumDir=${webViewEngine.offScreenEngine.options().chromiumDir()}")
    }
  }

  System.setProperty("apple.awt.application.name", "Dweb Browser");
  // https://github.com/JetBrains/kotlin-multiplatform-dev-docs/blob/master/topics/whats-new/whats-new-compose-1-6-0.md#desktop-experimental
  // 设置为WINDOW，则MenuPanel可以弹出到前面，而不会被webview遮挡
  System.setProperty("compose.layers.type", "WINDOW")
  // https://github.com/JetBrains/compose-multiplatform-core/pull/915
  if (PureViewController.isMacOS) {
    System.setProperty("compose.interop.blending", "true")
  }
  // https://github.com/JetBrains/compose-multiplatform/issues/1521
  // skiko.renderApi属性介绍：https://github.com/kropp/skiko/blob/master/skiko/src/jvmMain/kotlin/org/jetbrains/skiko/SkikoProperties.kt
  //  System.setProperty("skiko.renderApi", "SOFTWARE")
  if (PureViewController.isWindows) {
    // 需要开启这个属性，否则新版桌面的taskbar的composePanel无法在JDialog上渲染
    // see: https://youtrack.jetbrains.com/issue/CMP-5837/Improve-ComposePanel-rendering
    if (envSwitch.isEnabled(ENV_SWITCH_KEY.DESKTOP_STYLE_COMPOSE)) {
      System.setProperty("compose.swing.render.on.graphics", "true")
    }
    // 回退到OPENGL，不使用DIRECT3D，否则windows平台的swing操作会崩溃
//    System.setProperty("skiko.renderApi", "DIRECT3D")
  }
  // windows平台需要开启单实例，否则会打开多个桌面端
  if (PureViewController.isWindows && !WindowsSingleInstance.requestSingleInstance(
      args.joinToString(",")
    )
  ) {
    return
  }

  try {
    val dnsNMMDeferred = globalDefaultScope.async(ioAsyncExceptionHandler) {
      // 等待“应用”准备完毕
      PureViewController.awaitPrepared()
      // 启动内核
      startDwebBrowser(System.getenv("debug") ?: System.getProperty("debug"))
    }
    // 启动“应用”
    PureViewController.startApplication(hotReload = { content -> HotReload(content) }) {}
    dnsNMMDeferred.await().runtimeOrNull?.shutdown()
  } catch (e: Exception) {
    WARNING("global catch error : ${e.stackTraceToString()}")
  } finally {
    WARNING("exitProcess")
    exitProcess(0)
  }
}

val devtoolsItemTrayId = randomUUID()

@OptIn(InternalComposeApi::class)
@Composable
fun PrepareOffscreenWebCanvas(nativeMM: NativeMicroModule.NativeRuntime) {
  val offscreenWebCanvas = rememberOffscreenWebCanvas()
  LaunchedEffect(offscreenWebCanvas) {
    val webview = offscreenWebCanvas.webview
    val trayTitleFlow = MutableStateFlow(webview.title());
    webview.on(TitleChanged::class.java) {
      trayTitleFlow.value = it.title()
    }
//    webview.navigation().apply {
//      on(NavigationStarted::class.java) {
//        trayTitleFlow.value = "${it.url()} - ${it.navigation().browser().title()}"
//      }
//      on(NavigationFinished::class.java) {
//        trayTitleFlow.value = "${it.url()} - ${it.navigation().browser().title()}"
//      }
//    }
    registryDevtoolsTray(nativeMM, devtoolsItemTrayId, trayTitleFlow, openDevTool = {
      webview.devTools().show()
    }, onDestroy = { handler ->
      webview.on(BrowserClosed::class.java) {
        handler()
      }
    })
  }
}