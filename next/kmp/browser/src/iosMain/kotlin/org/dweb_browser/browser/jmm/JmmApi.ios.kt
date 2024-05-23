package org.dweb_browser.browser.jmm

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.std.file.ext.readFile
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.platform.NSDataHelper.toByteArray
import org.dweb_browser.helper.platform.NSDataHelper.toNSData
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSMutableData
import platform.Foundation.appendData

actual fun getChromeWebViewVersion(): String? {
  return null
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun jmmAppHashVerify(
  jmmNMM: JmmNMM.JmmRuntime,
  jmmMetadata: JmmMetadata,
  zipFilePath: String,
): Boolean {
  val data = NSMutableData()
  val deferred = CompletableDeferred<String>()
  jmmNMM.readFile(zipFilePath).stream().getReader("JmmAppHashVerify")
    .consumeEachArrayRange { byteArray, last ->
      if (!last) {
        data.appendData(byteArray.toNSData())
      } else {
        val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
        val input = data.toByteArray()
        input.usePinned { inputPinned ->
          digest.usePinned { digestPinned ->
            CC_SHA256(inputPinned.addressOf(0), input.size.convert(), digestPinned.addressOf(0))
          }
        }

        val hashValue = digest.joinToString("") {
          if (it < 16U) {
            "0" + it.toString(16)
          } else {
            it.toString(16)
          }
        }
        debugJMM("jmmAppHashVerify", "bundleHash=${jmmMetadata.manifest.bundle_hash}")
        debugJMM("jmmAppHashVerify", "zipFileHash=sha256:${hashValue}")
        deferred.complete(hashValue)
      }
    }

  return "sha256:${deferred.await()}" == jmmMetadata.manifest.bundle_hash
}