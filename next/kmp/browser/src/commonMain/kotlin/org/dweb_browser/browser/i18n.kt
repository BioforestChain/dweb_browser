package org.dweb_browser.browser

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.OneParamI18nResource
import org.dweb_browser.helper.compose.SimpleI18nResource
import org.dweb_browser.helper.toSpaceSize


class BrowserI18nResource {
  companion object {
    val dialog_title_webview_upgrade = SimpleI18nResource(Language.ZH to "更新提示")

    class WebViewVersions(var currentVersion: String, var requiredVersion: String)

    val dialog_text_webview_upgrade = OneParamI18nResource({ WebViewVersions("", "") },
      Language.ZH to { "当前系统中的 Android System Webview 版本过低 ($currentVersion)，所安装扩展软件可能无法正确运行。\n如果遇到此类情况，请优先将 Android System Webview 版本更新至 $requiredVersion 以上再重试。" })
    val dialog_confirm_webview_upgrade = SimpleI18nResource(Language.ZH to "确定")
    val dialog_dismiss_webview_upgrade = SimpleI18nResource(Language.ZH to "帮助文档")

    class InstallByteLength(var current: Long = 0, var total: Long = 0) {
      companion object {
        fun asI18nResource() = OneParamI18nResource({ InstallByteLength() })
      }
    }

    val install_button_download = InstallByteLength.asI18nResource()
      .define { Language.ZH to { "下载 (${total.toSpaceSize()})" } }
    val install_button_update = InstallByteLength.asI18nResource()
      .define { Language.ZH to { "更新 (${total.toSpaceSize()})" } }
    val install_button_downloading = InstallByteLength.asI18nResource()
      .define { Language.ZH to { "下载中 ${current.toSpaceSize()} / ${total.toSpaceSize()}" } }
    val install_button_paused = InstallByteLength.asI18nResource()
      .define { Language.ZH to { "暂停  ${current.toSpaceSize()} / ${total.toSpaceSize()}" } }
    val install_button_installing = SimpleI18nResource(Language.ZH to "安装中...")
    val install_button_open = SimpleI18nResource(Language.ZH to "打开")
    val install_button_retry = SimpleI18nResource(Language.ZH to "重新下载")
    val install_button_incompatible = SimpleI18nResource(Language.ZH to "该软件与您的设备不兼容")
    val no_download_links = SimpleI18nResource(Language.EN to "There are no download links yet", Language.ZH to "暂无下载数据")
    val manager_downloads = SimpleI18nResource(Language.EN to "Downloads", Language.ZH to "下载")
    val manager_files = SimpleI18nResource(Language.EN to "Files", Language.ZH to "文件")
  }
}