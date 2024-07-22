package org.dweb_browser.helper

import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.asSource
import kotlinx.io.buffered
import platform.Foundation.NSInputStream


fun NSInputStreamToByteReadChannel(
  scope: CoroutineScope,
  nsInputStream: NSInputStream
) = ByteReadChannel(source = nsInputStream.asSource().buffered())