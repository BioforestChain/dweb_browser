package info.bagen.dwebbrowser

import okio.Path.Companion.toPath
import okio.buffer
import org.dweb_browser.core.std.file.ResourceFileSystem
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test

class FileTest {
  @Test
  fun testResourceFs() = runCommonTest {

    val fs = ResourceFileSystem.FileSystem

    val textPath = "/hi.txt".toPath()
    val metadata = fs.metadataOrNull(textPath)
    println(metadata)
    if (metadata != null) {
      println(
        ">$textPath: ${
          fs.source(textPath).buffer().readByteArray().decodeToString()
        }"
      )
    }
  }

  @Test
  fun testPickerFs() = runCommonTest {

  }
}