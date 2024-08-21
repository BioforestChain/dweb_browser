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
import org.dweb_browser.sys.window.core.constant.WindowConstants
import org.dweb_browser.sys.window.core.modal.AlertModalState
import org.dweb_browser.sys.window.core.modal.BottomSheetsModalState
import org.dweb_browser.sys.window.core.modal.BottomSheetsModalState.Companion.createBottomSheetsModal
import org.dweb_browser.sys.window.core.modal.ModalState
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertIs


class ModalStateTest {
  class SimpleWindowController(
    override val lifecycleScope: CoroutineScope = CoroutineScope(defaultAsyncExceptionHandler),
  ) : WindowController(WindowState(WindowConstants(owner = "", ownerVersion = ""))) {
    override val viewBox: IPureViewBox get() = TODO("???")
  }

  @Test
  fun testJson() {
    val win = SimpleWindowController()
    val m1 = win.createBottomSheetsModal(title = "m1")
    val j1 = Json.encodeToString<ModalState>(m1)
    assertContains(j1, "bottom-sheet")
    val m11 = Json.decodeFromString<ModalState>(j1)
    assertIs<BottomSheetsModalState>(m11)

    val m2 = AlertModalState(title = "hi", message = "xxx")
    val j2 = Json.encodeToString<ModalState>(m2)
    assertContains(j2, "alert")
    val m22 = Json.decodeFromString<ModalState>(j2)
    assertIs<AlertModalState>(m22)
  }

  @OptIn(ExperimentalSerializationApi::class)
  @Test
  fun testCbor() {
    val win = SimpleWindowController()
    val m1 = win.createBottomSheetsModal(title = "m1")
    val j1 = Cbor.encodeToByteArray<ModalState>(m1)
    assertContains(j1.decodeToString(), "bottom-sheet")
    val m11 = Cbor.decodeFromByteArray<ModalState>(j1)
    assertIs<BottomSheetsModalState>(m11)

    val m2 = AlertModalState(title = "hi", message = "xxx")
    val j2 = Cbor.encodeToByteArray<ModalState>(m2)
    assertContains(j2.decodeToString(), "alert")
    val m22 = Cbor.decodeFromByteArray<ModalState>(j2)
    assertIs<AlertModalState>(m22)
  }
}