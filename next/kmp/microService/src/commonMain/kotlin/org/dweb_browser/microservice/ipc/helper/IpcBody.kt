package org.dweb_browser.microservice.ipc.helper

import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readBytes
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.byteArrayInputStream
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.toBase64
import org.dweb_browser.helper.toBase64ByteArray
import org.dweb_browser.helper.toUtf8
import org.dweb_browser.microservice.ipc.Ipc


fun debugIpcBody(tag: String, msg: Any = "", err: Throwable? = null) =
  printDebug("ipc-body", tag, msg, err)

abstract class IpcBody {
  /**
   * 缓存，这里不提供辅助函数，只是一个统一的存取地方，
   * 写入缓存者要自己维护缓存释放的逻辑
   */
  class CACHE {
    companion object {
      /**
       * 任意的 RAW 背后都会有一个 IpcBodySender/IpcBodyReceiver
       * 将它们缓存起来，那么使用这些 RAW 确保只拿到同一个 IpcBody，这对 RAW-Stream 很重要，流不可以被多次打开读取
       */
      val raw_ipcBody_WMap = WeakHashMap<Any, IpcBody>()

      /**
       * 每一个 metaBody 背后，都会有第一个 接收者IPC，这直接定义了它的应该由谁来接收这个数据，
       * 其它的 IPC 即便拿到了这个 metaBody 也是没有意义的，除非它是 INLINE
       */
      val metaId_receiverIpc_Map = mutableMapOf<String, Ipc>()

      /**
       * 每一个 metaBody 背后，都会有一个 IpcBodySender,
       * 这里主要是存储 流，因为它有明确的 open/close 生命周期
       */
      val metaId_ipcBodySender_Map = mutableMapOf<String, IpcBodySender>()
    }

  }


  protected inner class BodyHub {
    var base64: String? = null
    var stream: ByteReadPacket? = null
    var u8a: ByteArray? = null
    var data: Any? = null
  }

  protected abstract val bodyHub: BodyHub
  abstract val metaBody: MetaBody

  open val raw get() = bodyHub.data

  private val _u8a by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    (bodyHub.u8a ?: bodyHub.stream?.let {
      it.readBytes()
    } ?: bodyHub.base64?.toBase64ByteArray() ?: throw Exception("invalid body type")).also {
      CACHE.raw_ipcBody_WMap.put(it, this)
    }
  }

  suspend fun u8a() = this._u8a

  private val _stream by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    (bodyHub.stream ?: _u8a.let {
      it.byteArrayInputStream()
    }).also {
      CACHE.raw_ipcBody_WMap.put(it, this)
    }
  }

  fun stream() = this._stream

  private val _base64 by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    (bodyHub.base64 ?: _u8a.let {
      it.toBase64()
    }).also {
      CACHE.raw_ipcBody_WMap.put(it, this)
    }
  }

  fun base64() = this._base64

  private val _text by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    this._base64.toBase64ByteArray().toUtf8()
  }

  fun text() = this._text

}
