import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dweb_browser.helper.platform.PureViewController


fun main(): Unit = runBlocking {
  launch {
    // 等待“应用”准备完毕
    PureViewController.awaitPrepared()
    // 启动内核
    startDwebBrowser(true)
  }
  // 启动“应用”
  PureViewController.startApplication()
}
