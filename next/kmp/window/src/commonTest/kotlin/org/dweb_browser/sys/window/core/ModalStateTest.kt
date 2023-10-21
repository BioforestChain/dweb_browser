package org.dweb_browser.sys.window.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.toUtf8
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertIs


class ModalStateTest {
  @Test
  fun testJson() {
    val m1 = BottomSheetsModal("m1")
    val j1 = Json.encodeToString<ModalState>(m1)
    assertContains(j1, "bottom-sheet")
    val m11 = Json.decodeFromString<ModalState>(j1)
    assertIs<BottomSheetsModal>(m11)

    val m2 = AlertModal(title = "hi", message = "xxx")
    val j2 = Json.encodeToString<ModalState>(m2)
    assertContains(j2, "alert")
    val m22 = Json.decodeFromString<ModalState>(j2)
    assertIs<AlertModal>(m22)
  }

  @OptIn(ExperimentalSerializationApi::class)
  @Test
  fun testCbor() {
    val m1 = BottomSheetsModal("m1")
    val j1 = Cbor.encodeToByteArray<ModalState>(m1)
    assertContains(j1.toUtf8(), "bottom-sheet")
    val m11 = Cbor.decodeFromByteArray<ModalState>(j1)
    assertIs<BottomSheetsModal>(m11)

    val m2 = AlertModal(title = "hi", message = "xxx")
    val j2 = Cbor.encodeToByteArray<ModalState>(m2)
    assertContains(j2.toUtf8(), "alert")
    val m22 = Cbor.decodeFromByteArray<ModalState>(j2)
    assertIs<AlertModal>(m22)
  }
}