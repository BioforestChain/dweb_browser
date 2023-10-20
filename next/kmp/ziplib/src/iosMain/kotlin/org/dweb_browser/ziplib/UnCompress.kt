package org.dweb_browser.ziplib

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.cValuesOf
//import minizip.MZ_END_OF_LIST
//import minizip.MZ_MEM_ERROR
//import minizip.MZ_OK
//import minizip.mz_zip_reader_close
//import minizip.mz_zip_reader_create
//import minizip.mz_zip_reader_delete
//import minizip.mz_zip_reader_entry_cb
//import minizip.mz_zip_reader_open_file
//import minizip.mz_zip_reader_save_all

//import minizip.unzip
//import miniz.unzip
import miniz.zip_stream_open
import miniz.zip_extract
import miniz.zip_stream_extract


@OptIn(ExperimentalForeignApi::class)
actual fun unCompress(zipFilePath: String, destPath: String) : Boolean = zip_extract(zipFilePath, destPath, null, null) == 0

@OptIn(ExperimentalForeignApi::class)
fun unCompressStream() {
  val size: ULong = 0u
//  val a = zip_stream_open("", size, 0, 0)


//  zip_stream_extract(",=", size, "", a, null)
}
