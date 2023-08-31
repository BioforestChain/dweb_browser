package info.bagen.dwebbrowser.microService.browser.desk.types

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class DeskAppMetaDataTest {
  val data = DeskAppMetaData()

  @Test
  fun json() {
    println(data.toCommonAppManifest())
    val json = Json
      .encodeToString(data.toCommonAppManifest())
    println(json)
  }
}