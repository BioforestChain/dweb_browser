import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import com.teamdev.jxbrowser.browser.event.BrowserClosed
import com.teamdev.jxbrowser.browser.event.TitleChanged
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.dwebview.IDWebView.Companion.registryDevtoolsTray
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.image.compose.rememberOffscreenWebCanvas
import kotlin.system.exitProcess

suspend fun main(vararg args: String) {
  System.setProperty("apple.awt.application.name", "Dweb Browser");
  // https://github.com/JetBrains/kotlin-multiplatform-dev-docs/blob/master/topics/whats-new/whats-new-compose-1-6-0.md#desktop-experimental
  // 设置为WINDOW，则MenuPanel可以弹出到前面，而不会被webview遮挡
  System.setProperty("compose.layers.type", "WINDOW")
  // https://github.com/JetBrains/compose-multiplatform-core/pull/915
  System.setProperty("compose.interop.blending", "true")
  // https://github.com/JetBrains/compose-multiplatform/issues/1521
  //      System.setProperty("compose.swing.render.on.graphics", "true")
  //  System.setProperty("skiko.renderApi", "SOFTWARE")
  //  System.setProperty("skiko.renderApi", "OPENGL")
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
      startDwebBrowser(
        System.getenv("debug") ?: System.getProperty("debug"), listOf() //ExtMM(TrayNMM(), true)
      )
    }
    // 启动“应用”
    PureViewController.startApplication {
      val dnsNMM = produceState<NativeMicroModule.NativeRuntime?>(null) {
        value = dnsNMMDeferred.await().runtimeOrNull as NativeMicroModule.NativeRuntime?
      }.value
      if (dnsNMM != null) {
        PrepareOffscreenWebCanvas(dnsNMM)
      }
    }
    dnsNMMDeferred.await().runtimeOrNull?.shutdown()
  } catch (e: Exception) {
    WARNING("global catch error : ${e.message}")
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