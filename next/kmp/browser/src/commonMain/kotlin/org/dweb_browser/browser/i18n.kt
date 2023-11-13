package org.dweb_browser.browser

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.OneParamI18nResource
import org.dweb_browser.helper.compose.SimpleI18nResource


class BrowserI18nResource {
  companion object {
    val dialog_title_webview_upgrade = SimpleI18nResource(Language.ZH to "更新提示")

    class WebViewVersions(var currentVersion: String, var requiredVersion: String)

    val dialog_text_webview_upgrade = OneParamI18nResource(
      { WebViewVersions("", "") },
      Language.ZH to { "当前系统中的 Android System Webview 版本过低 ($currentVersion)，所安装扩展软件可能无法正确运行。\n如果遇到此类情况，请优先将 Android System Webview 版本更新至 $requiredVersion 以上再重试。" },
      Language.EN to { "The Android System Webview version in the current system is too low ($currentVersion), and the installed extension software may not run correctly.\nIf you encounter this situation, please upgrade the Android System Webview version to $requiredVersion or above and try again. " },
    )
    val dialog_confirm_webview_upgrade =
      SimpleI18nResource(Language.ZH to "确定", Language.ZH to "Confirm")
    val dialog_dismiss_webview_upgrade =
      SimpleI18nResource(Language.ZH to "帮助文档", Language.ZH to "Help")

    class InstallByteLength(var current: Long = 0, var total: Long = 0) {
      companion object {
        fun asI18nResource() = OneParamI18nResource({ InstallByteLength() })
      }
    }

    //    val install_button_download = InstallByteLength.asI18nResource()
//      .define { Language.ZH to { "下载 (${total.toSpaceSize()})" } }
//    val install_button_update = InstallByteLength.asI18nResource()
//      .define { Language.ZH to { "更新 (${total.toSpaceSize()})" } }
//    val install_button_downloading = InstallByteLength.asI18nResource()
//      .define { Language.ZH to { "下载中 ${current.toSpaceSize()} / ${total.toSpaceSize()}" } }
//    val install_button_paused = InstallByteLength.asI18nResource()
//      .define { Language.ZH to { "暂停  ${current.toSpaceSize()} / ${total.toSpaceSize()}" } }
    val install_button_download =
      SimpleI18nResource(Language.ZH to "下载", Language.EN to "DownLoad")
    val install_button_update = SimpleI18nResource(Language.ZH to "更新", Language.EN to "Upgrade")
    val install_button_downloading =
      SimpleI18nResource(Language.ZH to "下载中", Language.EN to "Downloading")
    val install_button_paused = SimpleI18nResource(Language.ZH to "暂停", Language.EN to "Pause")
    val install_button_installing = SimpleI18nResource(Language.ZH to "安装中...")
    val install_button_open = SimpleI18nResource(Language.ZH to "打开")
    val install_button_retry = SimpleI18nResource(Language.ZH to "重新下载")
    val install_button_incompatible = SimpleI18nResource(Language.ZH to "该软件与您的设备不兼容")
    val no_download_links = SimpleI18nResource(
      Language.EN to "There are no download links yet",
      Language.ZH to "暂无下载数据"
    )
    val no_apps_data = SimpleI18nResource(
      Language.EN to "There are no Apps",
      Language.ZH to "没有应用数据"
    )
    val manager_downloads = SimpleI18nResource(Language.EN to "Downloads", Language.ZH to "下载")
    val manager_files = SimpleI18nResource(Language.EN to "Files", Language.ZH to "文件")
    val unzip_button_install = SimpleI18nResource(Language.ZH to "安装", Language.EN to "Install")
    val unzip_button_cancel = SimpleI18nResource(Language.ZH to "取消", Language.EN to "Cancel")
    val unzip_title_no = SimpleI18nResource(Language.ZH to "编号", Language.EN to "No")
    val unzip_title_url =
      SimpleI18nResource(Language.ZH to "下载链接", Language.EN to "Download Link")
    val unzip_title_createTime =
      SimpleI18nResource(Language.ZH to "创建时间", Language.EN to "Create Time")
    val unzip_title_originMmid =
      SimpleI18nResource(Language.ZH to "下载来源", Language.EN to "Download App")
    val unzip_title_originUrl =
      SimpleI18nResource(Language.ZH to "数据来源", Language.EN to "Data Source")
    val unzip_title_mime = SimpleI18nResource(Language.ZH to "文件类型", Language.EN to "Mime")
    val button_name_open = SimpleI18nResource(Language.ZH to "打开", Language.EN to "Open")
    val button_name_confirm = SimpleI18nResource(Language.ZH to "确定", Language.EN to "Confirm")
    val button_name_cancel = SimpleI18nResource(Language.ZH to "取消", Language.EN to "Cancel")
    val top_bar_title_install =
      SimpleI18nResource(Language.ZH to "下载记录", Language.EN to "Download Record")
    val top_bar_title_download =
      SimpleI18nResource(Language.ZH to "下载列表", Language.EN to "All Downloads")
    val top_bar_title_down_detail =
      SimpleI18nResource(Language.ZH to "下载详情", Language.EN to "Download Detail")
  }
}