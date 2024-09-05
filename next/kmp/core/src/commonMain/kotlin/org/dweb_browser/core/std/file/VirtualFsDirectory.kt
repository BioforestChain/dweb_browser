package org.dweb_browser.core.std.file

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.dweb_browser.core.help.AdapterManager
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.io.SystemFileSystem

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

  val fs: FileSystem

  /**
   * 如果是文件系统
   * 获取真实的基本路径
   */
  fun resolveTo(remote: IMicroModuleManifest, virtualFullPath: Path): Path

  fun getFileHeaders(
    remote: IMicroModuleManifest,
    virtualFullPath: Path,
    trueFullPath: Path,
  ): PureHeaders = PureHeaders()
}

interface VirtualFsDirectory {
  fun isMatch(firstSegment: String): Boolean

  val fs: FileSystem

  /**
   * 如果是文件系统
   * 获取真实的基本路径
   */
  fun resolveTo(remote: IMicroModuleManifest, virtualFullPath: Path): Path

  fun getFileHeaders(
    remote: IMicroModuleManifest,
    vfsPath: VirtualFsPath,
  ): PureHeaders = PureHeaders()
}


/**
 * 一种通用的虚拟文件目录，需要提供真实的物理设备的
 */
fun commonVirtualFsDirectoryFactory(
  firstSegmentFlags: String,
  nativeFsPath: Path,
  separated: Boolean = true,
  fs: FileSystem = SystemFileSystem,
) = object : VirtualFsDirectory {
  override fun isMatch(firstSegment: String) = firstSegment == firstSegmentFlags
  override val fs: FileSystem = fs
  override fun resolveTo(remote: IMicroModuleManifest, virtualFullPath: Path): Path {
    val virtualFirstPath = "${virtualFullPath.root ?: "/"}$firstSegmentFlags".toPath()
    val virtualContentPath = virtualFullPath.relativeTo(virtualFirstPath)
    if (separated) {
      return nativeFsPath.resolve(remote.mmid).resolve(virtualContentPath)
    } else {
      return nativeFsPath.resolve(virtualContentPath)
    }
  }
}

fun commonVirtualFsDirectoryFactory(firstSegment: String, nativeFsPath: String) =
  commonVirtualFsDirectoryFactory(firstSegment, nativeFsPath.toPath())


val Path.first get() = "${root ?: " / "}${segments.first()}".toPath()

operator fun Path.plus(path: Path) = resolve(path)
operator fun Path.plus(path: String) = resolve(path.toPath())
operator fun Path.minus(path: Path) = relativeTo(path)
operator fun Path.minus(path: String) = relativeTo(path.toPath())


//
//class CommonVirtualFsDirectory(val firstSegment: String, val nativeFsPath: Path) : IVirtualFsDirectory {
//  override fun isMatch(segment: String) = firstSegment == segment
//  override fun getFsBasePath(remote: IMicroModuleManifest) = nativeFsPath.resolve(remote.mmid)
//}


/**
 * 获取应用内部目录
 */
expect fun FileNMM.Companion.getApplicationRootDir(): Path

/**
 * 获取应用缓存目录
 */
expect fun FileNMM.Companion.getApplicationCacheDir(): Path

/**
 * 持久化数据
 */
expect fun FileNMM.getDataVirtualFsDirectory(): VirtualFsDirectory


/**
 * 缓存文件夹，这里的空间会被按需回收
 */
expect fun FileNMM.getCacheVirtualFsDirectory(): VirtualFsDirectory

/**
 * 外部下载文件夹，这里的空间不会被回收
 */
expect fun FileNMM.getExternalDownloadVirtualFsDirectory(): VirtualFsDirectory


class FileDirectoryAdapterManager internal constructor() : AdapterManager<VirtualFsDirectory>()

val fileTypeAdapterManager = FileDirectoryAdapterManager()


