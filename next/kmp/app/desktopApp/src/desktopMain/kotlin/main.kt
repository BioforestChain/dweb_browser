import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.dweb_browser.helper.platform.PureViewController


fun main(): Unit = runBlocking {
//  application {
//    Window(onCloseRequest = ::exitApplication) {
//      Text("qaq")
//    }
//  }
  coroutineScope {
    startDwebBrowser()
  }
  PureViewController.startApplication()
}
