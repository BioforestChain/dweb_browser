package org.dweb_browser.sys.window.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.defaultAsyncExceptionHandler
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.toUtf8
import org.dweb_browser.sys.window.core.modal.BottomSheetsModal.Companion.createBottomSheetsModal
import org.dweb_browser.sys.window.core.constant.WindowConstants
import org.dweb_browser.sys.window.core.modal.AlertModal
import org.dweb_browser.sys.window.core.modal.BottomSheetsModal
import org.dweb_browser.sys.window.core.modal.ModalState
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertIs


class ModalStateTest {
  class SimpleWindowController(
    override val coroutineScope: CoroutineScope = CoroutineScope(defaultAsyncExceptionHandler)
  ) : WindowController(WindowState(WindowConstants(owner = "", ownerVersion = ""))) {
    override val viewController: IPureViewBox get() = TODO("???")
  }

  @Test
  fun testJson() {
    val win = SimpleWindowController()
    val m1 = win.createBottomSheetsModal(title = "m1")
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
    val win = SimpleWindowController()
    val m1 = win.createBottomSheetsModal(title = "m1")
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