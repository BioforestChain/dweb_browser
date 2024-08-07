import WindowsSingleInstance.singleInstanceFlow
import org.dweb_browser.browser.DwebBrowserLauncher
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.platform.DeepLinkHook
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.sys.tray.TrayNMM
import java.awt.Desktop


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
