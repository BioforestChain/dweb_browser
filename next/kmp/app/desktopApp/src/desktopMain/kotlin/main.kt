import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.PureViewController

fun main(vararg args: String): Unit = runBlocking {
//      // https://github.com/JetBrains/kotlin-multiplatform-dev-docs/blob/master/topics/whats-new/whats-new-compose-1-6-0.md#desktop-experimental
//      System.setProperty("compose.layers.type", "COMPONENT")
//      // https://github.com/JetBrains/compose-multiplatform-core/pull/915
  System.setProperty("compose.interop.blending", "true")
//      System.setProperty("compose.swing.render.on.graphics", "true")
//  System.setProperty("skiko.renderApi", "SOFTWARE")
//  System.setProperty("skiko.renderApi", "OPENGL")
  // windows平台需要开启单实例，否则会打开多个桌面端
  if(PureViewController.isWindows && !WindowsSingleInstance.requestSingleInstance(args.joinToString(","))) {
    return@runBlocking
  }

  CoroutineScope(ioAsyncExceptionHandler).launch {
    // 等待“应用”准备完毕
    PureViewController.awaitPrepared()
    // 启动内核
    startDwebBrowser(System.getenv("debug") ?: System.getProperty("debug"))
  }
  // 启动“应用”
  PureViewController.startApplication()
}
