package org.dweb_browser.core.ipc.stream

import io.ktor.utils.io.ByteReadChannel
import org.dweb_browser.helper.UUID

class Stream(
  override val id: UUID,
  override val totalSize: Number?,
  override val buffer: ByteReadChannel,
  override val pos: Number,
) : Source, Sink {
}