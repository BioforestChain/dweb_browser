package org.dweb_browser.microservice.std.file

import kotlinx.io.files.Path
import org.dweb_browser.helper.platform.PlatformViewController
import org.dweb_browser.microservice.help.types.IMicroModuleManifest


actual val dataFileDirectory = object :FileDirectory(){
  override fun isMatch(type: String?): Boolean {
    return type == "data"
  }

  override fun getDir(remote: IMicroModuleManifest): Path {
    PlatformViewController
  }

}
actual val cacheFileDirectory: FileDirectory