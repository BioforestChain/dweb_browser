package org.dweb_browser.pure.crypto.hash

import org.node.array.toByteArray
import org.node.array.toUint8Array

actual suspend fun sha256(data: ByteArray): ByteArray {
  return org.node.crypto.createHash("sha256").update(data.toUint8Array()).digest().toByteArray()
}

actual suspend fun sha256(data: String): ByteArray {
  return org.node.crypto.createHash("sha256").update(data.toJsString()).digest().toByteArray()
}