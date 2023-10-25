package org.dweb_browser.core.std.file

import kotlinx.serialization.Serializable


@Serializable
data class FileMetadata(
  val isFile: Boolean,
  val isDirectory: Boolean,
  val size: Long? = null,
  val createdTime: Long = 0,
  val lastReadTime: Long = 0,
  val lastWriteTime: Long = 0,
)