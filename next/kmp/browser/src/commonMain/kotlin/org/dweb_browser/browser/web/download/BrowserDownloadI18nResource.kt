package org.dweb_browser.browser.web.download

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

object BrowserDownloadI18nResource {
  val download_page_manage =
    SimpleI18nResource(Language.ZH to "下载管理", Language.EN to "Download Manager")
  val download_page_delete =
    SimpleI18nResource(Language.ZH to "选择项目", Language.EN to "Select Project")
  val download_page_complete =
    SimpleI18nResource(Language.ZH to "已下载", Language.EN to "Downloaded")

  val sheet_download = SimpleI18nResource(
    Language.ZH to "下载文件", Language.EN to "Download File"
  )
  val sheet_download_state_init = SimpleI18nResource(
    Language.ZH to "下载", Language.EN to "Download"
  )
  val sheet_download_state_resume = SimpleI18nResource(
    Language.ZH to "继续", Language.EN to "Resume"
  )
  val sheet_download_state_pause = SimpleI18nResource(
    Language.ZH to "暂停", Language.EN to "Pause"
  )
  val sheet_download_state_install = SimpleI18nResource(
    Language.ZH to "安装", Language.EN to "Install"
  )
  val sheet_download_state_open = SimpleI18nResource(
    Language.ZH to "打开", Language.EN to "Open"
  )

  val sheet_download_tip_exist = SimpleI18nResource(
    Language.ZH to "此文件已在下载列表中", Language.EN to "This file is already in the download list"
  )
  val sheet_download_tip_cancel = SimpleI18nResource(
    Language.ZH to "取消", Language.EN to "Cancel"
  )
  val sheet_download_tip_continue = SimpleI18nResource(
    Language.ZH to "继续下载", Language.EN to "Continue"
  )
  val sheet_download_tip_reload = SimpleI18nResource(
    Language.ZH to "重新下载", Language.EN to "Re-Download"
  )

  val tab_downloading = SimpleI18nResource(
    Language.ZH to "下载中", Language.EN to "Downloading"
  )
  val tab_downloaded = SimpleI18nResource(
    Language.ZH to "已下载", Language.EN to "Completed"
  )


  val button_delete = SimpleI18nResource(
    Language.ZH to "删除", Language.EN to "Delete"
  )

  val tip_empty = SimpleI18nResource(
    Language.ZH to "暂无下载任务和记录", Language.EN to "No Download Tasks And Records"
  )
}