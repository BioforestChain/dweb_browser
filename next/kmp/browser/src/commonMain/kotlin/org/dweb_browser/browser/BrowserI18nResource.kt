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
    val install_tab_download =
      SimpleI18nResource(Language.ZH to "下载", Language.EN to "DownLoad")
    val install_tab_file =
      SimpleI18nResource(Language.ZH to "文件", Language.EN to "File")
    val install_button_install = SimpleI18nResource(Language.ZH to "安装", Language.EN to "Install")
    val install_button_download =
      SimpleI18nResource(Language.ZH to "下载", Language.EN to "DownLoad")
    val install_button_update = SimpleI18nResource(Language.ZH to "升级", Language.EN to "Upgrade")
    val install_button_downloading =
      SimpleI18nResource(Language.ZH to "下载中", Language.EN to "Downloading")
    val install_button_paused = SimpleI18nResource(Language.ZH to "暂停", Language.EN to "Pause")
    val install_button_installing =
      SimpleI18nResource(Language.ZH to "安装中", Language.EN to "Install")
    val install_button_open = SimpleI18nResource(Language.ZH to "打开", Language.EN to "Open")
    val install_button_retry =
      SimpleI18nResource(Language.ZH to "重载失效资源", Language.EN to "Retry")
    val install_button_retry2 = SimpleI18nResource(Language.ZH to "重试", Language.EN to "Retry")
    val install_button_incompatible = SimpleI18nResource(
      Language.ZH to "该软件与您的设备不兼容",
      Language.EN to "The software is not compatible with your device"
    )
    val no_download_links = SimpleI18nResource(
      Language.ZH to "暂无下载数据", Language.EN to "There are no download links yet",
    )
    val no_apps_data = SimpleI18nResource(
      Language.ZH to "没有应用数据", Language.EN to "There are no Apps",
    )
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
      SimpleI18nResource(Language.ZH to "应用列表", Language.EN to "Application Manage")
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

    val browser_search_engine =
      SimpleI18nResource(Language.ZH to "搜索引擎", Language.EN to "Search Engine")
    val browser_search_title = SimpleI18nResource(Language.ZH to "搜索", Language.EN to "Search")
    val browser_search_cancel = SimpleI18nResource(Language.ZH to "取消", Language.EN to "Cancel")
    val browser_search_hint =
      SimpleI18nResource(Language.ZH to "搜索或输入网址", Language.EN to "Search or Input Website")
    val browser_empty_list = SimpleI18nResource(Language.ZH to "暂无数据", Language.EN to "No Data")
    val browser_options_editBook =
      SimpleI18nResource(Language.ZH to "编辑书签", Language.EN to "Edit Book")
    val browser_options_engine_list =
      SimpleI18nResource(Language.ZH to "搜索引擎", Language.EN to "Search Engine")
    val browser_options_engine_update =
      SimpleI18nResource(Language.ZH to "修改搜索引擎", Language.EN to "Update Search Engine")
    val browser_engine_tag_search =
      SimpleI18nResource(Language.ZH to "搜索引擎", Language.EN to "Search Engine")
    val browser_engine_tag_host =
      SimpleI18nResource(Language.ZH to "搜索域名", Language.EN to "Search Host")
    val browser_engine_tag_url = SimpleI18nResource(
      Language.ZH to "搜索格式（用“%s”代替搜索字词）",
      Language.EN to "Search Format(replace search terms with \"%s\")")
    val browser_engine_tips_noFound = SimpleI18nResource(
      Language.ZH to "未正确配置搜索引擎，请到“选项”中的“搜索引擎”中进行配置！",
      Language.EN to "Search engine not configured correctly, please go to \"Search Engine\" in \"Options\" to configure!")
    val browser_engine_toast_noFound = SimpleI18nResource(
      Language.ZH to "关键字搜索，需要通过搜索引擎，请先正确配置搜索引擎！",
      Language.EN to "Keyword search requires the use of search engines. Please configure the search engine correctly first!")
    val browser_options_store = SimpleI18nResource(Language.ZH to "存储", Language.EN to "Store")
    val browser_options_delete = SimpleI18nResource(Language.ZH to "删除", Language.EN to "Delete")
    val browser_options_addToBook =
      SimpleI18nResource(Language.ZH to "添加到书签", Language.EN to "Add To Book")
    val browser_options_share = SimpleI18nResource(Language.ZH to "分享", Language.EN to "Share")
    val browser_options_noTrace =
      SimpleI18nResource(Language.ZH to "无痕浏览", Language.EN to "NoTrace")
    val browser_options_privacy =
      SimpleI18nResource(Language.ZH to "隐私政策", Language.EN to "Privacy Policy")
    val browser_options_search_engine =
      SimpleI18nResource(Language.ZH to "搜索引擎", Language.EN to "Search Engines")
    val browser_multi_count = SimpleI18nResource(Language.ZH to "个标签页", Language.EN to "tabs")
    val browser_multi_done = SimpleI18nResource(Language.ZH to "完成", Language.EN to "Done")
    val browser_multi_startup = SimpleI18nResource(Language.ZH to "起始页", Language.EN to "Start")
    val browser_multi_no_title =
      SimpleI18nResource(Language.ZH to "无标题", Language.EN to "No Title")

    val toast_message_add_book =
      SimpleI18nResource(Language.ZH to "添加书签成功", Language.EN to "Add Book Success")
    val toast_message_add_book_invalid =
      SimpleI18nResource(Language.ZH to "无效链接", Language.EN to "Not Invalid Link")
    val toast_message_remove_book =
      SimpleI18nResource(Language.ZH to "移除书签成功", Language.EN to "Remove Book Success")
    val toast_message_update_book =
      SimpleI18nResource(Language.ZH to "修改书签成功", Language.EN to "Change Book Success")
    val toast_message_remove_history =
      SimpleI18nResource(Language.ZH to "移除历史记录成功", Language.EN to "Remove History Success")
    val toast_message_add_desk_success =
      SimpleI18nResource(Language.ZH to "添加到桌面成功", Language.EN to "Add to Desktop Success")
    val toast_message_add_desk_exist = SimpleI18nResource(
      Language.ZH to "桌面已存在该链接",
      Language.EN to "Desktop Already Exist The Link"
    )
    val toast_message_download_unzip_fail =
      SimpleI18nResource(Language.ZH to "安装失败", Language.EN to "Installed Fail")
    val toast_message_download_download_fail =
      SimpleI18nResource(Language.ZH to "下载失败", Language.EN to "Download Fail")

    val jmm_history_tab_installed =
      SimpleI18nResource(Language.ZH to "已安装", Language.EN to "Installed")
    val jmm_history_tab_uninstalled =
      SimpleI18nResource(Language.ZH to "未安装", Language.EN to "No Install")
    val jmm_install_version = SimpleI18nResource(Language.ZH to "版本", Language.EN to "version")
    val jmm_install_introduction =
      SimpleI18nResource(Language.ZH to "应用介绍", Language.EN to "Introduction")
    val jmm_install_update_log =
      SimpleI18nResource(Language.ZH to "更新日志", Language.EN to "Update Log")
    val jmm_install_info = SimpleI18nResource(Language.ZH to "信息", Language.EN to "Info")
    val jmm_install_info_dev =
      SimpleI18nResource(Language.ZH to "开发者", Language.EN to "Developer")
    val jmm_install_info_size = SimpleI18nResource(Language.ZH to "大小", Language.EN to "Size")
    val jmm_install_info_type = SimpleI18nResource(Language.ZH to "类别", Language.EN to "Type")
    val jmm_install_info_language =
      SimpleI18nResource(Language.ZH to "语言", Language.EN to "Language")
    val jmm_install_info_age = SimpleI18nResource(Language.ZH to "年龄", Language.EN to "Age")
    val jmm_install_info_copyright =
      SimpleI18nResource(Language.ZH to "版权", Language.EN to "CopyRight")
    val jmm_history_details = SimpleI18nResource(Language.ZH to "详情", Language.EN to "Details")
    val jmm_history_uninstall =
      SimpleI18nResource(Language.ZH to "卸载", Language.EN to "Uninstall")
  }
}