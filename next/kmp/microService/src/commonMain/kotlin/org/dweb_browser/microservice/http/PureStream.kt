package org.dweb_browser.microservice.http

import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

open class PureStream(private val readChannel: ByteReadChannel) {
  private var opened: String? = null
  val isOpened get() = opened != null
  private val openedDeferred = CompletableDeferred<Unit>()
  val afterOpened: Deferred<Unit> = openedDeferred
  fun getReader(reason: String): ByteReadChannel {
    if (opened != null) {
      throw Exception("stream already been read: $opened")
    }
    opened = reason
    openedDeferred.complete(Unit)
    return readChannel
  }

  override fun toString() = "PureStream[$readChannel]"

  fun toBody() = PureStreamBody(this)
}