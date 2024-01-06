package org.dweb_browser.pure.crypto.hash

actual fun sha256(data: ByteArray): ByteArray {
  return data.slice(0..32).toByteArray()
}