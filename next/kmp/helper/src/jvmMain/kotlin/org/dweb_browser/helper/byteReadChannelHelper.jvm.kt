package org.dweb_browser.helper

import io.ktor.utils.io.ByteReadChannel

public actual suspend inline fun ByteReadChannel.consumeEachArrayRange(visitor: ConsumeEachArrayVisitor): Unit =
  commonConsumeEachArrayRange(visitor)
