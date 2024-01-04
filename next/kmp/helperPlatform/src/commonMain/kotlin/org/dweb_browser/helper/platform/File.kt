package org.dweb_browser.helper.platform

import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MultiPartFileEncode {
  @SerialName("utf8")
  UTF8,

  @SerialName("base64")
  BASE64,

  @SerialName("binary")
  BINARY
}

@Serializable
data class MultiPartFile(
  val name: String,
  val size: Long,
  val type: String,
  val encoding: MultiPartFileEncode = MultiPartFileEncode.UTF8,
  val data: String,
)

enum class MultipartFileType {
  Desc,
  Data,
  End
}

@Serializable
data class MultipartFilePackage(val type: MultipartFileType, val chunk: ByteArray)

@Serializable
data class MultipartFieldDescription(
  val name: String?,
  val fileName: String?,
  val contentType: String?,
  val fieldIndex: Int
)

@Serializable
data class MultipartFieldData(
  val fieldIndex: Int,
  val chunk: ByteArray
)

@Serializable
data class MultipartFieldEnd(val fieldIndex: Int)

