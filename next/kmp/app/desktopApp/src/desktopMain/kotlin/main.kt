import kotlinx.coroutines.async
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.PureViewController
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
      args.joinToString(
        ","
      )
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
        System.getenv("debug") ?: System.getProperty("debug"),
        listOf() //ExtMM(TrayNMM(), true)
      )
    }
    // 启动“应用”
    PureViewController.startApplication()

    dnsNMMDeferred.await().runtimeOrNull?.shutdown()
  } catch (e: Exception) {
    WARNING("global catch error : $e")
    e.printStackTrace()
  } finally {
    exitProcess(0)
  }
}