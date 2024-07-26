package org.dweb_browser.browser.download

import org.dweb_browser.helper.compose.I18n

object DownloadI18n : I18n() {
  val completed = zh("已完成", "Completed")
  val downloading = zh("下载中", "Downloading")

  val no_select_detail = zh("未选择要展示详情的文件", "select file to show details")
  val unknown_origin = zh("未记录", "Unknown")
  val url_copy_success = zh("链接已复制", "Link copied")

  val unzip_label_no = zh("编号", "No")
  val unzip_label_name = zh("文件名", "File Name")
  val unzip_label_url = zh("下载链接", "Download Link")
  val unzip_label_path = zh("本地路径", "File Path")
  val unzip_label_createTime = zh("创建时间", "Create Time")
  val unzip_label_originUrl = zh("数据来源", "Origin")
  val unzip_label_originMmid = zh("下载器", "Downloader")
  val unzip_label_mime = zh("文件类型", "Content Type")

  val pause = zh("暂停", "Pause")
  val resume = zh("继续", "Resume")
  val paused = zh("已暂停", "Paused")
  val open = zh("打开", "Open")
  val retry = zh("重试", "Retry")
  val failed = zh("失败", "Failed")

  val delete_alert_title = zh("确定要删除下载任务？", "Confirm to delete the download task?")
  val delete_alert_message = zh1({ "文件：“$value”也会被同时从本地删除" },
    { "the file: \"$value\" will also be deleted from your disk" })
  val confirm_delete = zh("确认删除", "Delete!")
}