import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.PureViewController

/**
 * 程序执行锁
 */
private val running = atomic(false)
fun main(vararg args: String): Unit = runBlocking {
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
