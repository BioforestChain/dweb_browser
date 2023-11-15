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
    val time_today = SimpleI18nResource(Language.ZH to "今天", Language.EN to "Today")
    val time_yesterday = SimpleI18nResource(Language.ZH to "昨天", Language.EN to "Yesterday")

    val privacy_title = SimpleI18nResource(Language.ZH to "温馨提示", Language.EN to "Tips")
    val privacy_content_1 = SimpleI18nResource(
      Language.ZH to "欢迎使用 Dweb Browser，在您使用的时候，需要连接网络，产生的流量费用请咨询当地运营商。在使用 Dweb Browser 前，请认真阅读",
      Language.EN to "Welcome to Dweb Browser. It will use the internet when you use it. Please consult your local operator for traffic costs. Please read the"
    )
    val privacy_content_2 = SimpleI18nResource(
      Language.ZH to "。您需要同意并接受全部条款后再开始我们的服务。",
      Language.EN to "carefully before using Dweb Browser. You need to agree and accept all the terms before starting our service."
    )
    val privacy_policy =
      SimpleI18nResource(Language.ZH to "《隐私协议》", Language.EN to "《Privacy Policy》")
    val privacy_content_deny = SimpleI18nResource(
      Language.ZH to "本产品需要同意相关协议后才能使用哦",
      Language.EN to "This product can only be used after agreeing to the relevant agreement"
    )
    val privacy_button_refuse = SimpleI18nResource(Language.ZH to "不同意", Language.EN to "Refuse")
    val privacy_button_agree = SimpleI18nResource(Language.ZH to "同意", Language.EN to "Agree")
    val privacy_button_exit = SimpleI18nResource(Language.ZH to "退出", Language.EN to "Exit")
    val privacy_button_i_know = SimpleI18nResource(Language.ZH to "已知晓", Language.EN to "I Know")

    val browser_search_engine = SimpleI18nResource(Language.ZH to "搜索引擎", Language.EN to "Search Engine")
    val browser_search_title = SimpleI18nResource(Language.ZH to "搜索", Language.EN to "Search")
    val browser_search_cancel = SimpleI18nResource(Language.ZH to "取消", Language.EN to "Cancel")
    val browser_search_hint = SimpleI18nResource(Language.ZH to "搜索或输入网址", Language.EN to "Search or Input Website")
    val browser_empty_list = SimpleI18nResource(Language.ZH to "暂无数据", Language.EN to "No Data")
    val browser_options_editBook = SimpleI18nResource(Language.ZH to "编辑书签", Language.EN to "Edit Book")
    val browser_options_store = SimpleI18nResource(Language.ZH to "存储", Language.EN to "Store")
    val browser_options_delete = SimpleI18nResource(Language.ZH to "删除", Language.EN to "Delete")
    val browser_options_addToBook = SimpleI18nResource(Language.ZH to "添加到书签", Language.EN to "Add To Book")
    val browser_options_share = SimpleI18nResource(Language.ZH to "分享", Language.EN to "Share")
    val browser_options_noTrace = SimpleI18nResource(Language.ZH to "无痕浏览", Language.EN to "NoTrace")
    val browser_options_privacy = SimpleI18nResource(Language.ZH to "隐私政策", Language.EN to "Privacy Policy")
    val browser_multi_count = SimpleI18nResource(Language.ZH to "个标签页", Language.EN to "tabs")
    val browser_multi_done = SimpleI18nResource(Language.ZH to "完成", Language.EN to "Done")
    val browser_multi_startup = SimpleI18nResource(Language.ZH to "起始页", Language.EN to "Start")
    val browser_multi_no_title = SimpleI18nResource(Language.ZH to "无标题", Language.EN to "No Title")
  }
}