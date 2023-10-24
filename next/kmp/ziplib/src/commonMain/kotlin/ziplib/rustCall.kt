package ziplib

internal inline fun <U, E : Exception> rustCallWithError(
    errorHandler: CallStatusErrorHandler<E>,
    crossinline callback: (RustCallStatus) -> U
): U = withRustCallStatus {
    val returnValue = callback(it)
    if (it.isSuccess()) {
        returnValue
    } else if (it.isError()) {
        throw errorHandler.lift(it.errorBuffer)
    } else if (it.isPanic()) {
        if (it.errorBuffer.dataSize > 0) {
            throw InternalException(FfiConverterString.lift(it.errorBuffer))
        } else {
            throw InternalException("Rust panic")
        }
    } else {
        throw InternalException("Unknown rust call status: $it.code")
    }
}

interface CallStatusErrorHandler<E> {
    fun lift(error_buf: RustBuffer): E;
}

object NullCallStatusErrorHandler : CallStatusErrorHandler<InternalException> {
    override fun lift(error_buf: RustBuffer): InternalException {
        error_buf.free()
        return InternalException("Unexpected CALL_ERROR")
    }
}

internal inline fun <U> rustCall(crossinline callback: (RustCallStatus) -> U): U {
    return rustCallWithError(NullCallStatusErrorHandler, callback);
}