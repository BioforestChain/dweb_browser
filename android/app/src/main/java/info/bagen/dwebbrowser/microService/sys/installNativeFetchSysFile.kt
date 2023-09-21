package info.bagen.dwebbrowser.microService.sys

import info.bagen.dwebbrowser.App
import org.dweb_browser.microservice.sys.dns.RespondLocalFileContext.Companion.respondLocalFile
import org.dweb_browser.microservice.sys.dns.debugFetch
import org.dweb_browser.microservice.sys.dns.nativeFetchAdaptersManager
import org.dweb_browser.microservice.sys.dns.returnAndroidAsset

fun installNativeFetchSysFile() {
  nativeFetchAdaptersManager.append { fromMM, request ->
    return@append request.respondLocalFile {
      if (filePath.startsWith("/sys/")) {
        debugFetch("SysFile", "$fromMM => ${request.href}")
        returnAndroidAsset(App.appContext.assets, filePath.substring("/sys/".length))
      } else returnNext()
    }
  }
}