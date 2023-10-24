package ziplib

import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.ByReference
import okio.Buffer

@Structure.FieldOrder("capacity", "len", "data")
open class RustBufferStructure : Structure() {
    @JvmField
    var capacity: Int = 0

    @JvmField
    var len: Int = 0

    @JvmField
    var data: Pointer? = null
}

actual class RustBuffer : RustBufferStructure(), Structure.ByValue

actual class RustBufferPointer : ByReference(16) {
    fun setValueInternal(value: RustBuffer) {
        pointer.setInt(0, value.capacity)
        pointer.setInt(4, value.len)
        pointer.setPointer(8, value.data)
    }
}

actual fun RustBuffer.toBuffer(): Buffer =
    requireNotNull(data).getByteBuffer(0, len.toLong()).let { byteBuffer ->
        Buffer().also { it.write(byteBuffer) }
    }

actual val RustBuffer.dataSize: Int
    get() = len

actual fun RustBuffer.free() =
    rustCall { status ->
        UniFFILib.ffi_ziplib_d872_rustbuffer_free(this, status)
    }

actual fun allocRustBuffer(buffer: Buffer): RustBuffer =
    rustCall { status ->
        val size = buffer.size
        var readPosition = 0L
        UniFFILib.ffi_ziplib_d872_rustbuffer_alloc(size.toInt(), status).also { rustBuffer: RustBuffer ->
            val data = rustBuffer.data
                ?: throw RuntimeException("RustBuffer.alloc() returned null data pointer (size=${size})")
            rustBuffer.writeField("len", size.toInt())
            // Loop until the buffer is completed read, okio reads max 8192 bytes
            while (readPosition < size) {
                readPosition += buffer.read(data.getByteBuffer(readPosition, size - readPosition))
            }
        }
    }

actual fun RustBufferPointer.setValue(value: RustBuffer) = setValueInternal(value)

actual fun emptyRustBuffer(): RustBuffer = RustBuffer()