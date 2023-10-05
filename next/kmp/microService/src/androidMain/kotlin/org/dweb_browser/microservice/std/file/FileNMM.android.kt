package org.dweb_browser.microservice.std.file

import okio.Path.Companion.toPath
import org.dweb_browser.microservice.core.AndroidNativeMicroModule
import org.dweb_browser.microservice.help.types.IMicroModuleManifest


/**
 * 持久化数据
 */
actual fun FileNMM.getDataFileDirectory() = object : FileDirectory() {
  private val dataRootPath = AndroidNativeMicroModule.appContext.dataDir.absolutePath.toPath()
  override fun isMatch(type: String?) = type == null || type == "data"
  override fun getDir(remote: IMicroModuleManifest) = dataRootPath.resolve(remote.mmid)
}

/**
 * 缓存文件夹，这里的空间会被按需回收
 */
actual fun FileNMM.getCacheFileDirectory() = object : FileDirectory() {
  private val cacheRootPath = AndroidNativeMicroModule.appContext.cacheDir.absolutePath.toPath()
  override fun isMatch(type: String?) = type == "tmp" || type == "cache"
  override fun getDir(remote: IMicroModuleManifest) = cacheRootPath.resolve(remote.mmid)
}
