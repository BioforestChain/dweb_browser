package org.dweb_browser.browser.jmm

import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.consumeEachArrayRange
import java.security.MessageDigest

actual suspend fun jmmAppHashVerify(jmmNMM: JmmNMM, jmmHistoryMetadata: JmmHistoryMetadata, zipFilePath: String): Boolean {
  val messageDigest = MessageDigest.getInstance("SHA-256")
  val deferred = CompletableDeferred<String>()
  jmmNMM.nativeFetch("file://file.std.dweb/read?path=$zipFilePath").stream().getReader("JmmAppHashVerify").consumeEachArrayRange { byteArray, last ->
    if(!last) {
      messageDigest.update(byteArray)
    } else {
      val hashValue = messageDigest.digest().joinToString("") { "%02x".format(it) }
      debugJMM("jmmAppHashVerify", "bundleHash=${jmmHistoryMetadata.metadata.bundle_hash}")
      debugJMM("jmmAppHashVerify", "zipFileHash=sha256:${hashValue}")
      deferred.complete(hashValue)
    }
  }

  return "sha256:${deferred.await()}" == jmmHistoryMetadata.metadata.bundle_hash
}