package info.bagen.dwebbrowser

import okio.Path
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PathTest {
  @Test
  fun testUtil() {
    val virtualFsPath = "/data/a/b/c.json".toPath()
    assertTrue { virtualFsPath.isAbsolute }
    val virtualFirstSegment = virtualFsPath.segments.first()
    assertEquals("data", virtualFirstSegment)
    val virtualFirstPath = (Path.DIRECTORY_SEPARATOR + virtualFirstSegment).toPath()
    assertEquals("/data", virtualFirstPath.toString())


    val virtualContentPath = virtualFsPath.relativeTo(virtualFirstPath)
    assertEquals("a/b/c.json", virtualContentPath.toString())
    val fsBasePath = "/qwer/user.data".toPath()
    val fsFullPath = fsBasePath.resolve(virtualContentPath)
    assertEquals("/qwer/user.data/a/b/c.json", fsFullPath.toString())
  }
}