package org.dweb_browser.browser.download

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.MMID


data class DownloadTask(
  /** 下载编号 */
  val id: String,
  /** 下载链接 */
  val url: String,
  /** 文件路径 */
  val filepath: String,
  /** 创建时间 */
  val createTime: Long,
  /** 来源模块 */
  val originMmid: MMID,
  /** 来源链接 */
  val originUrl: String?,
  /** 下载回调链接 */
  val completeCallbackUrl: String?,
  /** 文件的元数据类型，可以用来做“打开文件”时的参考类型 */
  val mime: String,
)

@Serializable
enum class DownloadState {
  /** 初始化中，做下载前的准备，包括寻址、创建文件、保存任务等工作 */
  Init,

  /** 下载中*/
  Downloading,

  /** 暂停下载*/
  Paused,

  /** 取消下载*/
  Canceld,

  /** 下载失败*/
  Failed,

  /** 下载完成*/
  Completed,
}

@Serializable
data class DownloadProgressEvent(
  val id: String,
  val current: Long,
  val total: Long?,
  val state: DownloadState
)
