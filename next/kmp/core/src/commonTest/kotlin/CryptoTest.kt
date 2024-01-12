package info.bagen.dwebbrowser

import kotlinx.coroutines.runBlocking
import org.dweb_browser.core.std.file.ext.MicroModuleStore
import kotlin.test.Test
import kotlin.test.assertEquals

class CryptoTest {
  @Test
  fun testGCM() {
    runBlocking {
      val cipher = MicroModuleStore.getCipher("gaubee")
      val plaintext = "hi~"
      val ciphertext = cipher.encrypt(plaintext.encodeToByteArray())
      assertEquals(plaintext, cipher.decrypt(ciphertext).toUtf8())
    }
  }

}