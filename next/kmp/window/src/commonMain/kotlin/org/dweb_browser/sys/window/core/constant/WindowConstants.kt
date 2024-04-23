package org.dweb_browser.sys.window.core.constant

import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.randomUUID

/**
 * 窗口的不可变信息
 */
@Serializable
data class WindowConstants(
  /**
   * 窗口全局唯一编号，属于UUID的格式
   */
  val wid: UUID = randomUUID(),
  /**
   * 窗口持有者的元数据
   *
   * 窗口创建者
   */
  val owner: MMID,
  val ownerVersion: String,
  /**
   * 内容提提供方
   *
   * 比如若渲染的是web内容，那么应该是 mwebview.browser.dweb
   */
  val provider: MMID = owner,
) {
  /**
   * 提供放的 mm 实例
   */
  @Transient
  val microModule = mutableStateOf<MicroModule.Runtime?>(null)
}