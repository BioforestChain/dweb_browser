package org.dweb_browser.pure.http

import io.ktor.utils.io.ByteReadChannel
import io.ktor.util.toByteArray
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.helper.utf8String

class PureChannelMessage private constructor(private val source: Any, val isBinary: Boolean) {

  var binary: SuspendOnce<PureBinary> =
    SuspendOnce { throw Exception("invalid source for init binary") }
    private set
  var text: SuspendOnce<PureString> =
    SuspendOnce { throw Exception("invalid source for init text") }
    private set
  var stream: SuspendOnce<PureStream> =
    SuspendOnce { throw Exception("invalid source for init stream") }
    private set

  var asPureFrames: SuspendOnce<Flow<PureFrame>> =
    SuspendOnce { throw Exception("invalid source for init asPureFrames") }
    private set

  init {
    when (source) {
      is PureString -> {
        binary = SuspendOnce { source.utf8Binary }
        text = SuspendOnce { source }
        stream = SuspendOnce { PureStream(binary()) }
        asPureFrames = SuspendOnce { flow { emit(PureTextFrame(source)) } }
      }

      is PureBinary -> {
        binary = SuspendOnce { source }
        text = SuspendOnce { source.utf8String }
        stream = SuspendOnce { PureStream(source) }
        asPureFrames = SuspendOnce { flow { emit(PureBinaryFrame(source)) } }
      }

      is ByteReadChannel -> {
        binary = SuspendOnce { source.toByteArray() }
        text = SuspendOnce { binary().utf8String }
        stream = SuspendOnce { PureStream(source) }
        asPureFrames = SuspendOnce {
          flow {
            source.consumeEachArrayRange { byteArray, last ->
              emit(PureBinaryFrame(byteArray /*last*/))
            }
          }
        }
      }

      is PureStream -> {
        binary = SuspendOnce { source.getReader("to binary").toByteArray() }
        text = SuspendOnce { binary().utf8String }
        stream = SuspendOnce { source }
        asPureFrames = SuspendOnce {
          flow {
            source.getReader("as PureFrames").consumeEachArrayRange { byteArray, last ->
              emit(PureBinaryFrame(byteArray /*last*/))
            }
          }
        }
      }
    }
  }

  companion object {
    fun fromText(text: PureString) = PureChannelMessage(source = text, false)
    fun fromBinary(binary: PureBinary) = PureChannelMessage(source = binary, true)
    fun fromStream(stream: ByteReadChannel, isBinary: Boolean = true) =
      PureChannelMessage(source = stream, isBinary)

    fun fromStream(stream: PureStream, isBinary: Boolean = true) =
      PureChannelMessage(source = stream, isBinary)
  }
}