package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.asBase64
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.helper.toUtf8
import java.io.InputStream
import java.util.*


inline fun debugIpcBody(tag: String, msg: Any = "", err: Throwable? = null) =
    printdebugln("ipc-body", tag, msg, err)

abstract class IpcBody {
    companion object {
        val wm = WeakHashMap<Any, IpcBody>()
    }

    protected inner class BodyHub {
        var text: String? = null
        var stream: InputStream? = null
        var u8a: ByteArray? = null
        var data: Any? = null
    }

    protected abstract val bodyHub: BodyHub
    abstract val metaBody: MetaBody

    open val raw get() = bodyHub.data

    private val _u8a by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        (bodyHub.u8a ?: bodyHub.stream?.let {
            it.readBytes()
        } ?: bodyHub.text?.let {
            it.asBase64()
        } ?: throw Exception("invalid body type")).also {
            wm[it] = this
        }
    }

    suspend fun u8a() = this._u8a

    private val _stream by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        (bodyHub.stream ?: _u8a.let {
            it.inputStream()
        }).also {
            wm[it] = this
        }
    }

    fun stream() = this._stream

    private val _text by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        (bodyHub.text ?: _u8a.let {
            it.toUtf8()
        }).also {
            wm[it] = this
        }
    }

    fun text() = this._text

}
