import WindowsSingleInstance.singleInstanceFlow
import org.dweb_browser.browser.DwebBrowserLauncher
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.platform.DeepLinkHook
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.toByteArray
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.buildRequestX
import org.dweb_browser.sys.tray.TrayNMM
import java.awt.AWTEvent
import java.awt.Desktop
import java.awt.Robot
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.KeyEvent


suspend fun startDwebBrowser(debugTags: String?): DnsNMM {
  val launcher = DwebBrowserLauncher(
    debugTags, listOf(
      DwebBrowserLauncher.ExtMicroModule(
        factory = {
          TrayNMM()
        },
        boot = false,
        onSetup = { tray ->
          // 注册tray
          PureViewController.contents["tray"] = tray.getRender()
        },
      )
    )
  )

  val dnsRuntime = launcher.launch()
  dnsRuntime.open("tray.sys.dweb") // 由于快捷功能增加了截屏后，直接打开SmartScan功能，所以这边手动启动
  registerHotKey(dnsRuntime) // 注册热键

  // TODO fuck this
  DeepLinkHook.instance.onLink {
    println("deeplinkSignal => url=$it")
    dnsRuntime.nativeFetch(it)
  }

  // 添加dweb deeplinks处理
  try {
    Desktop.getDesktop().setOpenURIHandler { event ->
      if (event.uri.scheme == "dweb") {
        dnsRuntime.scopeLaunch(cancelable = true) {
          dnsRuntime.nativeFetch(event.uri.toString())
        }
      }
    }
  } catch (e: UnsupportedOperationException) {
    println("setOpenURIHandler is unsupported")
  }


  // 添加windows平台系統級dweb deeplinks处理
  if (PureViewController.isWindows) {
    singleInstanceFlow.collectIn(dnsRuntime.getRuntimeScope()) {
      dnsRuntime.nativeFetch(it)
    }
  }

  return launcher.dnsNMM
}

/**
 * 注册热键，快捷键
 */
private fun registerHotKey(dnsRuntime: DnsNMM.DnsRuntime) {
  val listener = AWTEventListener { event ->
    if (event is KeyEvent) {
      if (event.id == KeyEvent.KEY_RELEASED && event.keyCode == KeyEvent.VK_PRINTSCREEN && event.isAltDown) {
        // released => alt + printScreen
        dnsRuntime.scopeLaunch(cancelable = true) {
          val imageBitmap = PureViewController.awaitScreenCapture() // 打开截图
          dnsRuntime.nativeFetch(
            buildRequestX(
              url = "file://scan.browser.dweb/parseImage",
              method = PureMethod.POST,
              headers = PureHeaders(),
              body = imageBitmap.toByteArray()
            )
          )
        }
      }
    }
  }
  val toolkit = Toolkit.getDefaultToolkit()
  toolkit.addAWTEventListener(listener, AWTEvent.KEY_EVENT_MASK)
}
