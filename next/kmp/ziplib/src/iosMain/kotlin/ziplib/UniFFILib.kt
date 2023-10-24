package ziplib



actual object UniFFILib {
    init {
        
    }

    actual fun ziplib_d872_decompress(`zipFilePath`: RustBuffer,`destPath`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): ULong =
        requireNotNull(ziplib.cinterop.ziplib_d872_decompress(`zipFilePath`,`destPath`,
    _uniffi_out_err
        ))

    actual fun ffi_ziplib_d872_rustbuffer_alloc(`size`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer =
        requireNotNull(ziplib.cinterop.ffi_ziplib_d872_rustbuffer_alloc(`size`,
    _uniffi_out_err
        ))

    actual fun ffi_ziplib_d872_rustbuffer_from_bytes(`bytes`: ForeignBytes,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer =
        requireNotNull(ziplib.cinterop.ffi_ziplib_d872_rustbuffer_from_bytes(`bytes`,
    _uniffi_out_err
        ))

    actual fun ffi_ziplib_d872_rustbuffer_free(`buf`: RustBuffer,
    _uniffi_out_err: RustCallStatus
    ): Unit =
        requireNotNull(ziplib.cinterop.ffi_ziplib_d872_rustbuffer_free(`buf`,
    _uniffi_out_err
        ))

    actual fun ffi_ziplib_d872_rustbuffer_reserve(`buf`: RustBuffer,`additional`: Int,
    _uniffi_out_err: RustCallStatus
    ): RustBuffer =
        requireNotNull(ziplib.cinterop.ffi_ziplib_d872_rustbuffer_reserve(`buf`,`additional`,
    _uniffi_out_err
        ))

    
}