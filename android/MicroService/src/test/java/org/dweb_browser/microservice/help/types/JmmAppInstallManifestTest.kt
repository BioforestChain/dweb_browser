package org.dweb_browser.microservice.help.types

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class JmmAppInstallManifestTest {
  val data = JmmAppManifest.Default

  @Test
  fun json() {

    val json = Json.encodeToString(data)
    println("json: $json")

    val data1 = Json.decodeFromString<JmmAppManifest>(json)
    println("data1 :${data1}")
    println("data1 == data:${data1 == data}")

  }
}