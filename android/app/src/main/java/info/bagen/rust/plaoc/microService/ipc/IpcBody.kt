package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.asBase64
import info.bagen.rust.plaoc.microService.helper.toUtf8
import java.io.InputStream


open class IpcBody(
    type: IPC_DATA_TYPE,
    val rawBody: RawData,
    protected val ipc: Ipc
) : IpcMessage(type) {

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
            it.toUtf8()
        }
    }

    fun text() = this._text


}
