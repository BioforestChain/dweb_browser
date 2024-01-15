package org.dweb_browser.helper

import io.ktor.utils.io.ByteReadChannel

actual suspend inline fun ByteReadChannel.consumeEachArrayRange(visitor: ConsumeEachArrayVisitor) =
  commonConsumeEachArrayRange(visitor)