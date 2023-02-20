package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.asBase64
import io.ktor.utils.io.core.*
import java.io.InputStream


abstract class IpcBody {
    abstract val rawBody: RawData
    protected abstract val ipc: Ipc

    private inner class BodyHub {
        var text: String? = null
        var stream: InputStream? = null
        var u8a: ByteArray? = null
        var data: Any? = null
    }

    /// 因为是 abstract，所以得用 lazy 来延迟得到这些属性
    private val bodyHub by lazy {
        BodyHub().also {
            val data = rawDataToBody(rawBody, ipc)
            it.data = data
            when (data) {
                is String -> it.text = data;
                is ByteArray -> it.u8a = data
                is InputStream -> it.stream = data
            }
        }
    }


    val body get() = bodyHub.data

    private val _u8a by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        bodyHub.u8a ?: bodyHub.stream?.let {
            it.readBytes()
        } ?: bodyHub.text?.let {
            it.asBase64()
        } ?: throw Exception("invalid body type")
    }

    suspend fun u8a() = this._u8a

    private val _stream by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        bodyHub.stream ?: _u8a.let {
            it.inputStream()
        }
    }

    fun stream() = this._stream

    private val _text by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        bodyHub.text ?: _u8a.let {
            it.toString()
        }
    }

    suspend fun text() = this._text


}
