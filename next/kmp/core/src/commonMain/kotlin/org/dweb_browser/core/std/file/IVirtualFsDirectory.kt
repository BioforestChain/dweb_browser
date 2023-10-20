package org.dweb_browser.core.std.file

import okio.Path
import okio.Path.Companion.toPath
import org.dweb_browser.core.help.AdapterManager
import org.dweb_browser.core.help.types.IMicroModuleManifest

/**
 * 虚拟文件目录
 *
 * 这里我们强制根据路径的第一部分来进行匹配，比如`/data/a.json`就是取`data`来进行匹配
 *
 * 因为文件夹的特异性无法从外部很好地解决，所以需要内建一些基本的策略给到外部
 *
 * 比方说持久化数据和缓存数据
 */
interface IVirtualFsDirectory {
  fun isMatch(firstSegment: String): Boolean

  /**
   * 获取真实的基本路径
   */
  fun getFsBasePath(remote: IMicroModuleManifest): Path
}

/**
 * 一种通用的虚拟文件目录，需要提供真实的物理设备的
 */
fun commonVirtualFsDirectoryFactory(firstSegment: String, nativeFsPath: Path) =
  object : IVirtualFsDirectory {
    override fun isMatch(segment: String) = firstSegment == segment
    override fun getFsBasePath(remote: IMicroModuleManifest) = nativeFsPath.resolve(remote.mmid)
  }

fun commonVirtualFsDirectoryFactory(firstSegment: String, nativeFsPath: String) =
  commonVirtualFsDirectoryFactory(firstSegment, nativeFsPath.toPath())

//
//class CommonVirtualFsDirectory(val firstSegment: String, val nativeFsPath: Path) : IVirtualFsDirectory {
//  override fun isMatch(segment: String) = firstSegment == segment
//  override fun getFsBasePath(remote: IMicroModuleManifest) = nativeFsPath.resolve(remote.mmid)
//}


/**
 * 持久化数据
 */
expect fun FileNMM.getDataVirtualFsDirectory(): IVirtualFsDirectory


/**
 * 缓存文件夹，这里的空间会被按需回收
 */
expect fun FileNMM.getCacheVirtualFsDirectory(): IVirtualFsDirectory

/**
 * 用于picker时使用的临时文件夹
 */
expect fun FileNMM.getPickerVirtualFsDirectory(): IVirtualFsDirectory

expect fun FileNMM.unCompress(compressFile: String, unCompressDirectory: String)

class FileDirectoryAdapterManager internal constructor() : AdapterManager<IVirtualFsDirectory>()

val fileTypeAdapterManager = FileDirectoryAdapterManager()


