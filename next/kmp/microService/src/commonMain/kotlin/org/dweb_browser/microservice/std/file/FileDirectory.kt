package org.dweb_browser.microservice.std.file

import kotlinx.io.files.Path
import okio.Path
import org.dweb_browser.microservice.help.AdapterManager
import org.dweb_browser.microservice.help.types.IMicroModuleManifest

/**
 * 因为文件夹的特异性无法从外部很好地解决，所以需要内建一些基本的策略给到外部
 *
 * 比方说持久化数据和缓存数据
 */
abstract class FileDirectory {
  abstract fun isMatch(type: String?): Boolean
  abstract fun getDir(remote: IMicroModuleManifest): Path
//  /**
//   * 用户个人数据，这里的数据默认会被加密
//   */
//  Data("data"),
//
//  /**
//   * 缓存文件夹，这里的空间会被按需回收
//   */
//  Cache("cache"),
//
//  /**
//   * 下载文件夹，这里的文件意味着来自外部，因此不会被加密
//   */
//  Download("download"),
//
//  /**
//   * 共享文件夹，在这个文件夹中，每个文件或者文件夹都会有“权限密钥”，拥有密钥的模块可以根据密钥的权限进行读取或者写入
//   * 一般来说来自其它文件夹创建的 link
//   *
//   * > 可以使用这个文件夹实现共享程序，发起者创建一个可读写的文件夹，使用者分别在这个文件夹中创建自己可读写的文件，别人只读，那么模块们监听这个文件夹的文件变动，就可以做到数据共享
//   */
//  Share("share"),
//
//  /**
//   * 公开文件夹，任何模块都 可以进行读写（是否可写取决于创建者配置的权限）
//   */
//  Public("public"),

}
/**
 * 持久化数据
 */
expect fun FileNMM.getDataFileDirectory(): FileDirectory
/**
 * 缓存文件夹，这里的空间会被按需回收
 */
expect fun FileNMM.getDacheFileDirectory(): FileDirectory

class FileDirectoryAdapterManager internal constructor() : AdapterManager<FileDirectory>()

val fileTypeAdapterManager =FileDirectoryAdapterManager()