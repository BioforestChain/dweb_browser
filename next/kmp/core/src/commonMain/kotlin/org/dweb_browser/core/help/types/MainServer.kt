package org.dweb_browser.core.help.types

import kotlinx.serialization.Serializable

@Serializable
data class MainServer(
  /**
   * 应用文件夹的目录
   */
  val root: String,
  /**
   * 入口文件
   */
  val entry: String
)