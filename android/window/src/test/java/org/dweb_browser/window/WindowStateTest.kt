package org.dweb_browser.window

import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.window.core.WindowState
import org.dweb_browser.window.core.constant.WindowConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class WindowStateTest {
  val constants = WindowConstants("1", "owner", "provider")
  val state = WindowState(constants)

  @Test
  fun jsonConstants() {
    val jsonData = Json.encodeToString(constants)
    println("jsonData: $jsonData")
    val data1 = Json.decodeFromString<WindowConstants>(jsonData)
    println("data1: $data1")
    val jsonData1 = Json.encodeToString(data1)
    println("jsonData1: $jsonData1")
    assertEquals(jsonData, jsonData1)
  }

  @Test
  fun jsonState() {
    val jsonData = Json.encodeToString(state)
    println("jsonData: $jsonData")
    val data1 = Json.decodeFromString<WindowState>(jsonData)
    println("data1: $data1")
    val jsonData1 = Json.encodeToString(data1)
    println("jsonData1: $jsonData1")
    assertEquals(jsonData, jsonData1)
  }

  @Test
  fun cborState() {
    val cborData = Cbor.encodeToHexString(state)
    println("cborData: $cborData")
    val data1 = Cbor.decodeFromHexString<WindowState>(cborData)
    println("data1: $data1")
    val cborData1 = Cbor.encodeToHexString(data1)
    println("cborData1: $cborData1")
    assertEquals(cborData, cborData1)
  }
}