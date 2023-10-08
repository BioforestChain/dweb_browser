package info.bagen.dwebbrowser.microService.sys

import android.content.res.AssetManager
import info.bagen.dwebbrowser.App
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.std.dns.RespondLocalFileContext
import org.dweb_browser.microservice.std.dns.RespondLocalFileContext.Companion.respondLocalFile
import org.dweb_browser.microservice.std.dns.debugFetch
import org.dweb_browser.microservice.std.dns.nativeFetchAdaptersManager
import org.dweb_browser.microservice.sys.dns.parseToDirnameAndBasename
import org.dweb_browser.microservice.sys.dns.returnFile

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

fun RespondLocalFileContext.returnAndroidAsset(
  assetManager: AssetManager, assetPath: String = filePath
): PureResponse {
  val (dirname, basename) = assetPath.parseToDirnameAndBasename()
  /// 尝试打开文件，如果打开失败就走 404 no found 响应
  val filenameList = assetManager.list(dirname) ?: emptyArray()

  if (!filenameList.contains(basename)) {
    return returnNoFound()
  }

  val file = assetManager.open(
    "$dirname/$basename",
    if (preferenceStream) AssetManager.ACCESS_STREAMING else AssetManager.ACCESS_BUFFER
  )
  return returnFile(file)
}
