package ziplib



import com.sun.jna.Library
import com.sun.jna.Native

@Synchronized
private fun findLibraryName(): String {
    val componentName = "ziplib"
    val libOverride = System.getProperty("uniffi.component.$componentName.libraryOverride")
    if (libOverride != null) {
        return libOverride
    }
    return "ziplib"
}

actual object UniFFILib : Library {
    init {
        Native.register(UniFFILib::class.java, findLibraryName())
        
    }

    @JvmName("ziplib_d872_decompress")
    actual external fun ziplib_d872_decompress(`zipFilePath`: RustBuffer,`destPath`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): ULong

    @JvmName("ffi_ziplib_d872_rustbuffer_alloc")
    actual external fun ffi_ziplib_d872_rustbuffer_alloc(`size`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    @JvmName("ffi_ziplib_d872_rustbuffer_from_bytes")
    actual external fun ffi_ziplib_d872_rustbuffer_from_bytes(`bytes`: ForeignBytes,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    @JvmName("ffi_ziplib_d872_rustbuffer_free")
    actual external fun ffi_ziplib_d872_rustbuffer_free(`buf`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Unit

    @JvmName("ffi_ziplib_d872_rustbuffer_reserve")
    actual external fun ffi_ziplib_d872_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    
}