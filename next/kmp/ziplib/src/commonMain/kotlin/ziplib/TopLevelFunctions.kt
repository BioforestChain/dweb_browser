package ziplib



fun `decompress`(`zipFilePath`: String, `destPath`: String): ULong {
    return FfiConverterULong.lift(
    rustCall() {
    UniFFILib.ziplib_d872_decompress(FfiConverterString.lower(`zipFilePath`), FfiConverterString.lower(`destPath`), it)
})
}

