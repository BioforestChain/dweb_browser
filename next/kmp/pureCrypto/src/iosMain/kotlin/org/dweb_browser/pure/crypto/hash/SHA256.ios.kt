package org.dweb_browser.pure.crypto.hash

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256_CTX
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreCrypto.CC_SHA256_Final
import platform.CoreCrypto.CC_SHA256_Init
import platform.CoreCrypto.CC_SHA256_Update

actual suspend fun sha256(data: ByteArray) = common_sha256(data)
actual suspend fun sha256(data: String) = common_sha256(data)
actual fun sha256Sync(data: ByteArray) = ccSha256(data)


@OptIn(ExperimentalForeignApi::class)
fun ccSha256(data: ByteArray): ByteArray {
  val ctx = nativeHeap.alloc<CC_SHA256_CTX>().apply {
    CC_SHA256_Init(ptr)
  }
  data.usePinned { sourcePinned ->
    CC_SHA256_Update(ctx.ptr, sourcePinned.addressOf(0), data.size.convert())
  }
  return ByteArray(CC_SHA256_DIGEST_LENGTH).also { destination ->
    destination.asUByteArray().usePinned { destinationPinned ->
      CC_SHA256_Final(destinationPinned.addressOf(0), ctx.ptr)
    }
    nativeHeap.free(ctx.ptr)
  }
}
