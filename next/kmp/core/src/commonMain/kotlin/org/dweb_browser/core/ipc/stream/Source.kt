package org.dweb_browser.core.ipc.stream

import io.ktor.utils.io.ByteReadChannel
import org.dweb_browser.helper.UUID

// 读取器
interface Source {
  val id: UUID
  val totalSize: Number? // 总量
  val buffer: ByteReadChannel
  val pos: Number // 偏移量
}

