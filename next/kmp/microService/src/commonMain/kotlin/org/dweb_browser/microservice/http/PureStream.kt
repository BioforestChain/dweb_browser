package org.dweb_browser.microservice.http

import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

open class PureStream(private val readChannel: ByteReadChannel) {
  private var opened = false
  val isOpened get() = opened
  private val openedDeferred = CompletableDeferred<Unit>()
  val afterOpened: Deferred<Unit> = openedDeferred
  fun getReader(): ByteReadChannel {
    if (opened) {
      throw Exception("stream already been read")
    }
    opened = true
    openedDeferred.complete(Unit)
    return readChannel
  }

  override fun toString() = "PureStream[$readChannel]"

  fun toBody() = PureStreamBody(this)
}