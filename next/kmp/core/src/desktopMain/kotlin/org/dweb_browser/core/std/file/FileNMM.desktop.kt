package org.dweb_browser.core.std.file


/**
 * 持久化数据
 */
actual fun FileNMM.getDataVirtualFsDirectory() =
  commonVirtualFsDirectoryFactory("data", "/data/dweb-browser")

/**
 * 缓存文件夹，这里的空间会被按需回收
 */
actual fun FileNMM.getCacheVirtualFsDirectory() =
  commonVirtualFsDirectoryFactory("cache", "/cache/dweb-browser")

actual fun FileNMM.unCompress(compressFile:String,unCompressDirectory:String) {

}
