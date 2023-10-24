package ziplib



expect object UniFFILib {
    fun ziplib_d872_decompress(`zipFilePath`: RustBuffer,`destPath`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): ULong

    fun ffi_ziplib_d872_rustbuffer_alloc(`size`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    fun ffi_ziplib_d872_rustbuffer_from_bytes(`bytes`: ForeignBytes,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    fun ffi_ziplib_d872_rustbuffer_free(`buf`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Unit

    fun ffi_ziplib_d872_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer

    
}