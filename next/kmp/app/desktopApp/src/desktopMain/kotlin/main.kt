import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.PureViewController

/**
 * 程序执行锁
 */
private val running = atomic(false)
fun main(): Unit = runBlocking {
  /// 只允许执行一次
  running.getAndUpdate {
    if (it) {
      return@runBlocking
    }
    true
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
