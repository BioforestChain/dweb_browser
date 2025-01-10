package org.dweb_browser.browser

import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.compose.I18n
import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.OneParamI18nResource

object BrowserI18nResource : I18n() {
  val application_name = zh("Dweb Browser", "Dweb Browser")
  val dialog_title_webview_upgrade = zh("更新提示", "Update tip")

  class WebViewVersions(var currentVersion: String, var requiredVersion: String)

  val dialog_text_webview_upgrade = OneParamI18nResource(
    { WebViewVersions("", "") },
    Language.ZH to { "当前系统中的 Android System Webview 版本过低 ($currentVersion)，所安装扩展软件可能无法正确运行。\n如果遇到此类情况，请优先将 Android System Webview 版本更新至 $requiredVersion 以上再重试。" },
    Language.EN to { "The Android System Webview version in the current system is too low ($currentVersion), and the installed extension software may not run correctly.\nIf you encounter this situation, please upgrade the Android System Webview version to $requiredVersion or above and try again. " },
  )
  val dialog_confirm_webview_upgrade =
    zh("确定", "Confirm")
  val dialog_dismiss_webview_upgrade =
    zh("帮助文档", "Help")

  val install_button_install = zh("安装", "Install")
  val install_button_update = zh("升级", "Upgrade")
  val install_button_downloading = zh("下载中", "Downloading")
  val install_button_paused = zh("暂停", "Pause")
  val install_button_installing = zh("安装中", "Installing")
  val install_button_open = zh("打开", "Open")
  val install_button_lower = zh("版本降级", "Downgrade")
  val install_button_retry = zh("重载失效资源", "Retry")
  val install_button_retry2 = zh("重试", "Retry")
  val install_button_incompatible = zh(
    "软件不兼容", "Software Incompatible"
  )
  val install_button_jump_home = zh(
    "打开来源页", "Go to Referring Page"
  )
  val install_tooltip_warning = zh("警告", "WARING")

  val install_tooltip_lower_version_tip = zh(
    "该目标版本低于当前已安装的版本，确定要进行降级安装吗？",
    "The target version is lower than the currently installed version. Are you sure you want to downgrade?"
  )

  val install_tooltip_install_lower_action = zh("降级安装", "Downgrade and install")

  val no_download_links = zh("暂无下载数据", "There are no download links yet")
  val no_apps_data = zh("没有应用数据", "There are no Apps")
  val button_name_confirm = zh("确定", "Confirm")
  val button_name_cancel = zh("取消", "Cancel")
  val top_bar_title_download = zh("下载列表", "All Downloads")
  val top_bar_title_down_detail = zh("下载详情", "Download Detail")
  val time_today = zh("今天", "Today")
  val time_yesterday = zh("昨天", "Yesterday")
  val privacy_title = zh("温馨提示", "Tips")
  val privacy_content = zh(
    "欢迎使用 Dweb Browser，在您使用的时候，需要连接网络，产生的流量费用请咨询当地运营商。\n在使用 Dweb Browser 前，请认真阅读《隐私协议》。您需要同意并接受全部条款后再开始使用该软件。",
    "Welcome to Dweb Browser. Please note that an internet connection is required for use. Please consult your local operator for any data charges that may apply.\nBefore using Dweb Browser, please carefully read the 'Privacy Policy'. You must agree to and accept all terms and conditions before using the software."
  )
  val privacy_policy =
    zh("《隐私协议》", "'Privacy Policy'")
  val privacy_content_deny = zh(
    "本产品需要同意相关协议后才能使用哦",
    "This product can only be used after agreeing to the relevant agreement"
  )
  val privacy_button_refuse = zh("不同意", "Refuse")
  val privacy_button_agree = zh("同意", "Agree")
  val privacy_button_exit = zh("退出", "Exit")
  val privacy_button_i_know = zh("已知晓", "I Know")

  val browser_short_name = zh("浏览器", "Browser")
  val browser_search_ai = zh("AI", "AI")
  val browser_search_web2 = zh("Web2", "Web2")
  val browser_search_web_page = zh("网页", "Web Page")
  val browser_search_web3 = zh("Web3", "Web3")
  val browser_search_local_resources = zh("本地资源", "Local Resources")
  val browser_search_engine = zh("搜索引擎", "Search Engine")
  val browser_search_dwebapp = zh("Dweb应用", "Search Engine")
  val browser_search_title = zh("搜索", "Search")
  val browser_search_hint = zh("搜索或输入网址", "Search or Input Website")
  val browser_empty_list = zh("暂无数据", "No Data")
  val browser_bookmark_edit_dialog_title = zh("编辑书签", "Edit Bookmark")
  val browser_search_keyword = zh1({ "搜索 “$value”" }, { "Search '${value}'" })
  val browser_engine_inactive = zh("未启用", "Inactive")
  val browser_search_noFound = zh(
    "未检索到符合关键字的本地资源", "No local resource matching the keyword was retrieved"
  )
  val browser_search_comingSoon = zh("即将开放", "Coming Soon")
  val browser_web3_found_dwebapps = zh1({ "找到了 ${value}个 dweb应用" },
    { "Found $value dweb application${if (value == "1") "s" else ""}" })
  val browser_web3_search_logs = zh("搜索日志", "Search Logs")

  object Web3Search : I18n() {
    val preview_logs_lines = zh1({ "总共 $value 条日志" }, { "$value logs in total" })
    val log_start_dwebapps = zh("正在搜索 Dweb应用", "Scraping Dwebapps")
    val log_end = zh("搜索结束", "End of search")
    val log_fetch_dwebapps = zh1({ "加载 $value" }, { "fetching $value" })
    val log_parse_dwebapps = zh1({ "分析 $value" }, { "parsing $value" })
    val log_fail_dwebapps = zh1({ "无效 $value" }, { "unable $value" })
    val log_discover_dwebapps =
      zh2({ "发现 $value1 Dweb应用 入口: $value2" }, { "Discover $value1 Dwebapp portal: $value2" })
    val log_error_cors_dwebapps =
      zh1({ "失败 $value Dweb应用存在跨域限制" }, { "Fail $value dwebapp cors limit" })
    val log_error_integrity_dwebapps =
      zh1({ "警告 $value 正确性验证不通过" }, { "Warn $value integrity hash not match" })
    val log_error_dwebapps = zh2({ "错误 $value1 : $value2" }, { "Error $value1 : $value2" })
    val log_success_found_dwebapps =
      zh2({ "成功 $value1 找到 Dweb应用: $value2" }, { "Success $value1 found dwebapp: $value2" })
  }

  val browser_bookmark_title = zh("书签标题", "Bookmark Title")
  val browser_bookmark_url = zh("链接地址", "Bookmark Url")
  val browser_add_bookmark = zh("添加书签", "Add Bookmark")
  val browser_remove_bookmark = zh("移除书签", "Remove Bookmark")
  val browser_options_share = zh("分享", "Share")
  val browser_options_noTrace = zh("无痕浏览", "NoTrace")
  val browser_options_privacy = zh("隐私政策", "Privacy Policy")
  val browser_menu_scanner = zh("扫一扫", "Scan")
  val browser_menu_add_to_desktop = zh("添加到桌面", "Add To Desktop")
  val browser_web_go_back = zh("后退", "Go Back")
  val browser_web_go_forward = zh("前进", "Go Forward")
  val browser_web_refresh = zh("刷新", "Refresh")
  val browser_multi_count = zh("个标签页", "tabs")
  val browser_multi_done = zh("完成", "Done")
  val search_short_name = zh("搜索引擎", "Search")

  val toast_message_add_bookmark = zh("添加书签成功", "Add Bookmark Success")
  val toast_message_remove_bookmark = zh("移除书签成功", "Remove Bookmark Success")
  val toast_message_update_bookmark = zh("修改书签成功", "Change Bookmark Success")
  val toast_message_add_desk_success = zh("添加到桌面成功", "Add to Desktop Success")
  val toast_message_add_desk_exist = zh(
    "桌面已存在该链接", "Desktop Already Exist The Link"
  )
  val toast_message_download_unzip_fail = zh("安装失败", "Installed Fail")
  val toast_message_download_download_fail = zh("下载失败", "Download Fail")
  val toast_message_download_downloading = zh("正在下载中...", "Downloading...")

  object JsProcess {
    val short_name = zh("程序运行时", "App Runtime")
  }

  object JsMM {
    class CanNotSupportTargetParams(
      var appName: String = "",
      var appId: MMID = "",
      var currentVersion: Int = -1,
      var minTarget: Int = -1,
      var maxTarget: Int = -1,
    )

    val canNotSupportMinTarget = OneParamI18nResource({ CanNotSupportTargetParams() },
      Language.ZH to { "应用：$appName($appId) 与容器版本不匹配，当前版本：${currentVersion}，应用最低要求：${minTarget}" },
      Language.EN to { "App: $appName($appId) is incompatible with the container version. Current version: ${currentVersion}, app minimum requirement: ${minTarget}." })
    val canNotSupportMaxTarget = OneParamI18nResource({ CanNotSupportTargetParams() },
      Language.ZH to { "应用：$appName($appId) 与容器版本不匹配，当前版本：${currentVersion}，应用最高兼容到：${maxTarget}" },
      Language.EN to { "App: $appName($appId) is incompatible with the container version. Current version: ${currentVersion}, app maximum compatibility: ${maxTarget}." })

  }

  val download_shore_name = zh("下载管理", "Download Manager")

  val dialog_version_title = zh("版本更新", "Version Upgrade")
  val dialog_downloading_title = zh("下载中...", "Downloading...")
  val dialog_install_title = zh("安装提醒", "Install Reminder")
  val dialog_upgrade_description =
    zh("发现新版本，请及时更新！", "Find a new version, please update!")
  val dialog_install_description = zh(
    "安装应用需要授权，请移步授权，再尝试！",
    "Authorization is required to install the application, please move to authorization and try again!"
  )
  val dialog_upgrade_button_upgrade = zh("升级", "Upgrade")
  val dialog_upgrade_button_delay = zh("推迟", "Defer")
  val dialog_upgrade_button_background = zh("后台下载", "Background")
  val dialog_upgrade_button_setting = zh("设置", "Setting")

  object Home {
    val page_title = zh("起始页", "Home Page")
    val search_error = zh("没有与您搜索相关的数据！", "No data relevant to your search！")
  }

  object Web {
    val page_title = zh("网页", "Web")
    val web_page_loading = zh("加载中……", "Loading...")
  }

  object Bookmark {
    val page_title = zh("书签", "Bookmark")
    val tip_edit = zh("点击书签可以进行修改", "You can edit in dialog by tap bookmark")
  }

  object History {
    val page_title = zh("历史记录", "History Record")
  }

  object Engine {
    val page_title = zh("搜索引擎", "Engines")
    val status_enable = zh("开启", "Enable")
    val status_disable = zh("关闭", "Disable")
  }

  object Download {
    val page_title = zh("下载内容", "Downloads")

    val page_title_manage = zh("下载管理", "Download Manager")
    val unknownSize = zh("未知", "unknown")
    val dialog_download_title = zh("下载文件", "Download File")

    val dialog_retry_title = zh("是否重新下载文件？", "Do you want to re-download the file?")

    val dialog_retry_message = zh(
      "您想再次下载 %s ((%s)) 吗？", "Would you like to download %s ((%s)) again?"
    )

    val dialog_confirm = zh("再次下载", "Download Again")
    val dialog_cancel = zh("取消", "Cancel")

    val button_title_init = zh("下载", "Download")
    val button_title_resume = zh("继续", "Resume")
    val button_title_pause = zh("暂停", "Pause")
    val button_title_install = zh("安装", "Install")
    val button_title_open = zh("打开", "Open")
    val sheet_download_tip_cancel = zh("取消", "Cancel")
    val sheet_download_tip_continue = zh("继续下载", "Continue")
    val sheet_download_tip_reload = zh("重新下载", "Re-Download")

    val tip_empty = zh("暂无下载任务和记录", "No Download Tasks And Records")
    val dropdown_delete = zh("删除", "Delete")
    val dropdown_share = zh("分享", "Share")
    val dropdown_rename = zh("重命名", "Rename")
    val chip_all = zh("所有", "All")
    val chip_image = zh("图片", "Photo")
    val chip_video = zh("视频", "Video")
    val chip_audio = zh("音频", "Audio")
    val chip_doc = zh("文档", "Documents")
    val chip_package = zh("压缩包", "Archives")
    val chip_other = zh("其它", "Other")
  }

  object Setting {
    val page_title = zh("设置", "Setting")
  }

  object QRCode {
    val short_name = zh("智能扫码", "Smart Scan")
    val select_QR_code = zh("选择一张二维码图片", "Select a QR code image")
    val toast_mismatching = zh("无法解析的数据 -> %s", "no support data -> %s")
    val permission_tip_camera_title = zh("授权相机权限", "Authorize camera permissions")
    val permission_tip_camera_message = zh(
      "DwebBrowser正在向您获取“相机”权限，同意后，将用于为您提供扫描二维码服务",
      "DwebBrowser is asking you for \"Camera\" permissions, and if you agree, it will be used to provide you with scanning QR code services"
    )

    val permission_denied = zh(
      "获取“相机”权限失败，无法提供扫码服务",
      "Failed to obtain the Camera permission and cannot provide the code scanning service"
    )

    val noFoundWindow = zh(
      "无法找到承载的窗口信息",
      "Failed to obtain the Camera permission and cannot provide the code scanning service"
    )

    val Action = zh("打开", "Open")

    val emptyResult = zh("没有识别到条形码", "No barcode recognized")

    val confirm = zh("确定", "Confirm")
    val dismiss = zh("关闭", "Dismiss")
    val Back = zh("返回", "Back")
    val photo_album = zh("相册", "Album")
    val photo_endoscopic = zh("内窥", "Endoscopic")
    val simulator_title = zh("在模拟器上不可用", "not available on simulator")
    val simulator_body = zh(
      """
        相机在模拟器上不可用。
        请尝试在真正的iOS设备上运行。 """.trimIndent(), """ Camera is not available on simulator.
      Please try to run on a real iOS device.
      """
    )
    val webcam_detected_title = zh("没有检测到摄像头", "Camera not detected")
    val webcam_detected_body = zh("是否切换为内窥模式？", "Switch to Endoscopic Mode?")
    val tip_no_camera = zh(
      "没有检测到摄像头，请连接摄像头或者通过文件选择器选择图片",
      "No camera detected, please connect camera or use file selector"
    )
  }

  object IconDescription {
    val verified = zh("已认证", "Verified")

    val unverified = zh("未认证", "Unverified")
  }

  object Desk {
    val short_name = zh("我的桌面", "My Desk")
  }

  object Desktop {
    val quit = zh("退出", "Close")

    val detail = zh("应用详情", "App Info")

    val delete = zh("删除", "Delete")

    val share = zh("分享", "Share")
  }
}