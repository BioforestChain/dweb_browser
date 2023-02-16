package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.asBase64
import java.io.InputStream

open class IpcBody(open val rawBody: RawData, open val ipc: Ipc) {
    val body = run {
        rawDataToBody(rawBody, ipc).also { data ->
            when (data) {
                is String -> this._body_text = data
                is ByteArray -> this._body_u8a = data
                is InputStream -> this._body_stream = data
            }
        }
    }
    private var _body_u8a: ByteArray? = null;
    private var _body_stream: InputStream? = null;
    private var _body_text: String? = null;

    private val _u8a by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        _body_u8a ?: _body_stream?.let {
            it.readBytes()
        } ?: _body_text?.let {
            it.asBase64()
        } ?: throw Exception("invalid body type")
    }

    suspend fun u8a() = this._u8a

    private val _stream by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        _body_stream ?: _u8a.let {
            it.inputStream()
        }
    }

    suspend fun stream() = this._stream

    private val _text by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        _body_text ?: _u8a.let {
            it.toString()
        }
    }

    suspend fun text() = this._text


}
