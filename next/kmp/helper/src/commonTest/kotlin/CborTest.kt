package info.bagen.dwebbrowser

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.CborLoose
import kotlin.test.Test

class CborTest {

  @Serializable
  class IpcMessageTest(val name: String, val type: Int)

  @OptIn(ExperimentalSerializationApi::class)
  @Test
  fun `cbor_encodeToByteArray`() {
    println("cbor test111")
    val cborByteArray =
      CborLoose.encodeToByteArray(IpcMessageTest.serializer(), IpcMessageTest("test", 1))
    println(cborByteArray)
    val decodeValue = CborLoose.decodeFromByteArray(IpcMessageTest.serializer(), cborByteArray)
    println(decodeValue.type)
  }
}