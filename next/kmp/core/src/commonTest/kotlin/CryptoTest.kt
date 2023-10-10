package info.bagen.dwebbrowser

import kotlinx.coroutines.runBlocking
import org.dweb_browser.helper.toUtf8
import org.dweb_browser.helper.toUtf8ByteArray
import org.dweb_browser.core.std.file.ext.MicroModuleStore
import kotlin.test.Test
import kotlin.test.assertEquals

class CryptoTest {
  @Test
  fun testGCM() {
    runBlocking {
      val cipher = MicroModuleStore.getCipher("gaubee")
      val plaintext = "hi~"
      val ciphertext = cipher.encrypt(plaintext.toUtf8ByteArray())
      assertEquals(plaintext, cipher.decrypt(ciphertext).toUtf8())
    }
  }

}