package org.dweb_browser.window.core.constant

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.help.MMID

/**
 * 窗口的不可变信息
 */
@Serializable
data class WindowConstants(
  /**
   * 窗口全局唯一编号，属于UUID的格式
   */
  val wid: UUID = java.util.UUID.randomUUID().toString(),
  /**
   * 窗口持有者的元数据
   *
   * 窗口创建者
   */
  val owner: MMID,
  /**
   * 内容提提供方
   *
   * 比如若渲染的是web内容，那么应该是 mwebview.browser.dweb
   */
  val provider: MMID,
  /**
   * 提供放的 mm 实例
   */
  @Transient
  val microModule: MicroModule? = null,
)