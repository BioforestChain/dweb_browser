package org.dweb_browser.core.std.file

import io.ktor.http.HttpStatusCode
import okio.Path
import okio.Path.Companion.toPath
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.http.router.ResponseException

/**
 * 虚拟的路径映射
 */
class VirtualFsPath(
  context: IMicroModuleManifest,
  virtualPathString: String,
  findVfsDirectory: (firstSegment: String) -> VirtualFsDirectory?,
) {
  val virtualFullPath = virtualPathString.toPath(true).also {
    if (!it.isAbsolute) {
      throw ResponseException(
        HttpStatusCode.BadRequest, "File path should be absolute path"
      )
    }
    if (it.segments.isEmpty()) {
      throw ResponseException(
        HttpStatusCode.BadRequest, "File path required one segment at least"
      )
    }
  }
  val virtualFirstSegment = virtualFullPath.segments.first()
  val vfsDirectory = findVfsDirectory(virtualFirstSegment) ?: throw ResponseException(
    HttpStatusCode.NotFound, "No found top-folder: $virtualFirstSegment"
  )
  val fsFullPath = vfsDirectory.resolveTo(context, virtualFullPath)
  val fs = vfsDirectory.fs

  val virtualFirstPath by lazy {
    virtualFullPath.first
  }
  val fsFirstPath by lazy {
    vfsDirectory.resolveTo(context, virtualFirstPath)
  }

  /**
   *  virtualFirstPath.resolve(fsPath.relativeTo(fsFirstPath))
   */
  fun toVirtualPath(fsPath: Path) = virtualFirstPath + (fsPath - fsFirstPath)
  fun toVirtualPathString(fsPath: Path) = toVirtualPath(fsPath).toString()
}