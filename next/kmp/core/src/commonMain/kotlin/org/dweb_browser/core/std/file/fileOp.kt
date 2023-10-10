package org.dweb_browser.core.std.file

import kotlinx.serialization.Serializable

@Serializable
sealed class FileOp<O>(
  val type: FileOpType,
) {
  fun output(res: O) = res
}

enum class FileOpType {
  Size,//
  Read,//
  Write,//
  Append,//
  Close,//
  ;
}

@Serializable
class FileOpSize : FileOp<Long>(FileOpType.Size)

@Serializable
class FileOpRead(val input: Pair<Long?, Long?>) :
  FileOp<ByteArray>(FileOpType.Read)

@Serializable
class FileOpWrite(val input: Pair<Long?, ByteArray>) :
  FileOp<Unit>(FileOpType.Write)

@Serializable
class FileOpAppend(val input: ByteArray) :
  FileOp<Unit>(FileOpType.Append)

@Serializable
class FileOpClose() :
  FileOp<Unit>(FileOpType.Close)
