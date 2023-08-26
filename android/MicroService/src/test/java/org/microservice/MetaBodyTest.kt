package org.dweb_browser.microservice.ipc.helper

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class MetaBodyTest {
  @Test
  fun stringData() {
    val data = MetaBody.fromText(1, "data~", metaId = "qaq")
    val jsonString = Json.encodeToString(data)
    assertEquals("""{"type":3,"senderUid":1,"data":"data~","metaId":"qaq"}""", jsonString)
    val data1 = Json.decodeFromString<MetaBody>(jsonString)
    assertEquals(data, data1)
  }

  @Test
  fun binaryData() {
    val data = MetaBody.fromBinary(1, byteArrayOf(1, 2, 3, 4, 5, 6), metaId = "qaq")
    val jsonString = Json.encodeToString(data)
    assertEquals("""{"type":5,"senderUid":1,"data":"AQIDBAUG","metaId":"qaq"}""", jsonString)
    val data1 = Json.decodeFromString<MetaBody>(jsonString)
    assertEquals(data.jsonAble.toMetaBody(), data1)
  }
}