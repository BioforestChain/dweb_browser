package org.dweb_browser.helper

import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel

public fun createByteChannel(): ByteChannel = ByteChannel(true)


/**
 * 如果需要对 ByteReadChannel 进行重写，请继承该 Interface，否则底层在做 case 的时候会出问题
 * 比如native 的 joinTo、copyTo函数，需要进行 (this as ByteChannelSequentialBase) 操作，这时候使用代理是不行的，因此需要拿到 sourceByteReadChannel 来操作
 */
public interface ByteReadChannelDelegate {
  public val sourceByteReadChannel: ByteReadChannel
}