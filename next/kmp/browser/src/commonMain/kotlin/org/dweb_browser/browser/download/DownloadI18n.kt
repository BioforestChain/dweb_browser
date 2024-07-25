package org.dweb_browser.browser.download

import org.dweb_browser.helper.compose.I18n

object DownloadI18n : I18n() {
  val tab_completed = zh("已完成", "Completed")
  val tab_downloading = zh("下载中", "Downloading")

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
}