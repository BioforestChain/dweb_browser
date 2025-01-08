package org.dweb_browser.browser

import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.compose.I18n
import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.OneParamI18nResource
import org.dweb_browser.helper.compose.SimpleI18nResource

object BrowserI18nResource : I18n() {
  val application_name =
    SimpleI18nResource(Language.ZH to "Dweb Browser", Language.EN to "Dweb Browser")
  val dialog_title_webview_upgrade =
    SimpleI18nResource(Language.ZH to "更新提示", Language.EN to "Update tip")

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

  val install_button_install = SimpleI18nResource(Language.ZH to "安装", Language.EN to "Install")
  val install_button_update = SimpleI18nResource(Language.ZH to "升级", Language.EN to "Upgrade")
  val install_button_downloading =
    SimpleI18nResource(Language.ZH to "下载中", Language.EN to "Downloading")
  val install_button_paused = SimpleI18nResource(Language.ZH to "暂停", Language.EN to "Pause")
  val install_button_installing =
    SimpleI18nResource(Language.ZH to "安装中", Language.EN to "Installing")
  val install_button_open = SimpleI18nResource(Language.ZH to "打开", Language.EN to "Open")
  val install_button_lower =
    SimpleI18nResource(Language.ZH to "版本降级", Language.EN to "Downgrade")
  val install_button_retry =
    SimpleI18nResource(Language.ZH to "重载失效资源", Language.EN to "Retry")
  val install_button_retry2 = SimpleI18nResource(Language.ZH to "重试", Language.EN to "Retry")
  val install_button_incompatible = SimpleI18nResource(
    Language.ZH to "软件不兼容",
    Language.EN to "Software Incompatible"
  )
  val install_button_jump_home = SimpleI18nResource(
    Language.ZH to "打开来源页",
    Language.EN to "Go to Referring Page"
  )
  val install_tooltip_warning =
    SimpleI18nResource(Language.ZH to "警告", Language.EN to "WARING")

  val install_tooltip_lower_version_tip =
    SimpleI18nResource(
      Language.ZH to "该目标版本低于当前已安装的版本，确定要进行降级安装吗？",
      Language.EN to "The target version is lower than the currently installed version. Are you sure you want to downgrade?"
    )

  val install_tooltip_install_lower_action = SimpleI18nResource(
    Language.ZH to "降级安装",
    Language.EN to "Downgrade and install"
  )

  val no_download_links = SimpleI18nResource(
    Language.ZH to "暂无下载数据", Language.EN to "There are no download links yet",
  )
  val no_apps_data = SimpleI18nResource(
    Language.ZH to "没有应用数据", Language.EN to "There are no Apps",
  )
  val button_name_confirm = SimpleI18nResource(Language.ZH to "确定", Language.EN to "Confirm")
  val button_name_cancel = SimpleI18nResource(Language.ZH to "取消", Language.EN to "Cancel")
  val top_bar_title_download =
    SimpleI18nResource(Language.ZH to "下载列表", Language.EN to "All Downloads")
  val top_bar_title_down_detail =
    SimpleI18nResource(Language.ZH to "下载详情", Language.EN to "Download Detail")
  val time_today = SimpleI18nResource(Language.ZH to "今天", Language.EN to "Today")
  val time_yesterday = SimpleI18nResource(Language.ZH to "昨天", Language.EN to "Yesterday")

  val privacy_title = SimpleI18nResource(Language.ZH to "温馨提示", Language.EN to "Tips")
  val privacy_content = SimpleI18nResource(
    Language.ZH to "欢迎使用 Dweb Browser，在您使用的时候，需要连接网络，产生的流量费用请咨询当地运营商。\n在使用 Dweb Browser 前，请认真阅读《隐私协议》。您需要同意并接受全部条款后再开始使用该软件。",
    Language.EN to "Welcome to Dweb Browser. Please note that an internet connection is required for use. Please consult your local operator for any data charges that may apply.\nBefore using Dweb Browser, please carefully read the 'Privacy Policy'. You must agree to and accept all terms and conditions before using the software."
  )
  val privacy_policy =
    SimpleI18nResource(Language.ZH to "《隐私协议》", Language.EN to "'Privacy Policy'")
  val privacy_content_deny = SimpleI18nResource(
    Language.ZH to "本产品需要同意相关协议后才能使用哦",
    Language.EN to "This product can only be used after agreeing to the relevant agreement"
  )
  val privacy_button_refuse = SimpleI18nResource(Language.ZH to "不同意", Language.EN to "Refuse")
  val privacy_button_agree = SimpleI18nResource(Language.ZH to "同意", Language.EN to "Agree")
  val privacy_button_exit = SimpleI18nResource(Language.ZH to "退出", Language.EN to "Exit")
  val privacy_button_i_know = SimpleI18nResource(Language.ZH to "已知晓", Language.EN to "I Know")

  val browser_short_name = SimpleI18nResource(Language.ZH to "浏览器", Language.EN to "Browser")
  val browser_search_ai =
    SimpleI18nResource(Language.ZH to "AI", Language.EN to "AI")
  val browser_search_web2 =
    SimpleI18nResource(Language.ZH to "Web2", Language.EN to "Web2")
  val browser_search_web_page =
    SimpleI18nResource(Language.ZH to "网页", Language.EN to "Web Page")
  val browser_search_web3 =
    SimpleI18nResource(Language.ZH to "Web3", Language.EN to "Web3")
  val browser_search_local_resources =
    SimpleI18nResource(Language.ZH to "本地资源", Language.EN to "Local Resources")
  val browser_search_engine =
    SimpleI18nResource(Language.ZH to "搜索引擎", Language.EN to "Search Engine")
  val browser_search_dwebapp =
    SimpleI18nResource(Language.ZH to "Dweb应用", Language.EN to "Search Engine")
  val browser_search_title = SimpleI18nResource(Language.ZH to "搜索", Language.EN to "Search")
  val browser_search_hint =
    SimpleI18nResource(Language.ZH to "搜索或输入网址", Language.EN to "Search or Input Website")
  val browser_empty_list = SimpleI18nResource(Language.ZH to "暂无数据", Language.EN to "No Data")
  val browser_bookmark_edit_dialog_title =
    SimpleI18nResource(Language.ZH to "编辑书签", Language.EN to "Edit Bookmark")
  val browser_search_keyword = zh1(
    { "搜索 “$value”" },
    { "Search '${value}'" }
  )
  val browser_engine_inactive = zh("未启用", "Inactive")
  val browser_search_noFound = SimpleI18nResource(
    Language.ZH to "未检索到符合关键字的本地资源",
    Language.EN to "No local resource matching the keyword was retrieved"
  )
  val browser_search_comingSoon =
    SimpleI18nResource(Language.ZH to "即将开放", Language.EN to "Coming Soon")

  val browser_bookmark_title =
    SimpleI18nResource(Language.ZH to "书签标题", Language.EN to "Bookmark Title")
  val browser_bookmark_url =
    SimpleI18nResource(Language.ZH to "链接地址", Language.EN to "Bookmark Url")
  val browser_add_bookmark =
    SimpleI18nResource(Language.ZH to "添加书签", Language.EN to "Add Bookmark")
  val browser_remove_bookmark =
    SimpleI18nResource(Language.ZH to "移除书签", Language.EN to "Remove Bookmark")
  val browser_options_share = SimpleI18nResource(Language.ZH to "分享", Language.EN to "Share")
  val browser_options_noTrace =
    SimpleI18nResource(Language.ZH to "无痕浏览", Language.EN to "NoTrace")
  val browser_options_privacy =
    SimpleI18nResource(Language.ZH to "隐私政策", Language.EN to "Privacy Policy")
  val browser_menu_scanner =
    SimpleI18nResource(Language.ZH to "扫一扫", Language.EN to "Scan")
  val browser_menu_add_to_desktop =
    SimpleI18nResource(Language.ZH to "添加到桌面", Language.EN to "Add To Desktop")

  val browser_web_go_back = SimpleI18nResource(Language.ZH to "后退", Language.EN to "Go Back")
  val browser_web_go_forward =
    SimpleI18nResource(Language.ZH to "前进", Language.EN to "Go Forward")
  val browser_web_refresh = SimpleI18nResource(Language.ZH to "刷新", Language.EN to "Refresh")

  val browser_multi_count = SimpleI18nResource(Language.ZH to "个标签页", Language.EN to "tabs")
  val browser_multi_done = SimpleI18nResource(Language.ZH to "完成", Language.EN to "Done")
  val search_short_name = SimpleI18nResource(Language.ZH to "搜索引擎", Language.EN to "Search")

  val toast_message_add_bookmark =
    SimpleI18nResource(Language.ZH to "添加书签成功", Language.EN to "Add Bookmark Success")
  val toast_message_remove_bookmark =
    SimpleI18nResource(Language.ZH to "移除书签成功", Language.EN to "Remove Bookmark Success")
  val toast_message_update_bookmark =
    SimpleI18nResource(Language.ZH to "修改书签成功", Language.EN to "Change Bookmark Success")
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
  val toast_message_download_downloading =
    SimpleI18nResource(Language.ZH to "正在下载中...", Language.EN to "Downloading...")

  object JsProcess {
    val short_name =
      SimpleI18nResource(Language.ZH to "程序运行时", Language.EN to "App Runtime")
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

  val download_shore_name =
    SimpleI18nResource(Language.ZH to "下载管理", Language.EN to "Download Manager")

  val dialog_version_title = SimpleI18nResource(
    Language.ZH to "版本更新", Language.EN to "Version Upgrade"
  )
  val dialog_downloading_title = SimpleI18nResource(
    Language.ZH to "下载中...", Language.EN to "Downloading..."
  )
  val dialog_install_title = SimpleI18nResource(
    Language.ZH to "安装提醒", Language.EN to "Install Reminder"
  )
  val dialog_upgrade_description = SimpleI18nResource(
    Language.ZH to "发现新版本，请及时更新！", Language.EN to "Find a new version, please update!"
  )
  val dialog_install_description = SimpleI18nResource(
    Language.ZH to "安装应用需要授权，请移步授权，再尝试！",
    Language.EN to "Authorization is required to install the application, please move to authorization and try again!"
  )
  val dialog_upgrade_button_upgrade = SimpleI18nResource(
    Language.ZH to "升级", Language.EN to "Upgrade"
  )
  val dialog_upgrade_button_delay = SimpleI18nResource(
    Language.ZH to "推迟", Language.EN to "Defer"
  )
  val dialog_upgrade_button_background = SimpleI18nResource(
    Language.ZH to "后台下载", Language.EN to "Background"
  )
  val dialog_upgrade_button_setting = SimpleI18nResource(
    Language.ZH to "设置", Language.EN to "Setting"
  )

  object Home {
    val page_title = SimpleI18nResource(Language.ZH to "起始页", Language.EN to "Home Page")
    val search_error = SimpleI18nResource(
      Language.ZH to "没有与您搜索相关的数据！",
      Language.EN to "No data relevant to your search！"
    )
  }

  object Web {
    val page_title = SimpleI18nResource(Language.ZH to "网页", Language.EN to "Web")
    val web_page_loading =
      SimpleI18nResource(Language.ZH to "加载中……", Language.EN to "Loading...")
  }

  object Bookmark {
    val page_title = SimpleI18nResource(Language.ZH to "书签", Language.EN to "Bookmark")
    val tip_edit = SimpleI18nResource(
      Language.ZH to "点击书签可以进行修改",
      Language.EN to "You can edit in dialog by tap bookmark"
    )
  }

  object History {
    val page_title = SimpleI18nResource(Language.ZH to "历史记录", Language.EN to "History Record")
  }

  object Engine {
    val page_title = SimpleI18nResource(Language.ZH to "搜索引擎", Language.EN to "Engines")
    val status_enable = SimpleI18nResource(Language.ZH to "开启", Language.EN to "Enable")
    val status_disable = SimpleI18nResource(Language.ZH to "关闭", Language.EN to "Disable")
  }

  object Download {
    val page_title =
      SimpleI18nResource(Language.ZH to "下载内容", Language.EN to "Downloads")

    val page_title_manage =
      SimpleI18nResource(Language.ZH to "下载管理", Language.EN to "Download Manager")

    val unknownSize = SimpleI18nResource(Language.ZH to "未知", Language.EN to "unknown")

    val dialog_download_title = SimpleI18nResource(
      Language.ZH to "下载文件",
      Language.EN to "Download File"
    )

    val dialog_retry_title = SimpleI18nResource(
      Language.ZH to "是否重新下载文件？",
      Language.EN to "Do you want to re-download the file?"
    )

    val dialog_retry_message = SimpleI18nResource(
      Language.ZH to "您想再次下载 %s ((%s)) 吗？",
      Language.EN to "Would you like to download %s ((%s)) again?"
    )

    val dialog_confirm = SimpleI18nResource(
      Language.ZH to "再次下载", Language.EN to "Download Again"
    )
    val dialog_cancel = SimpleI18nResource(
      Language.ZH to "取消", Language.EN to "Cancel"
    )

    val button_title_init = SimpleI18nResource(
      Language.ZH to "下载", Language.EN to "Download"
    )
    val button_title_resume = SimpleI18nResource(
      Language.ZH to "继续", Language.EN to "Resume"
    )
    val button_title_pause = SimpleI18nResource(
      Language.ZH to "暂停", Language.EN to "Pause"
    )
    val button_title_install = SimpleI18nResource(
      Language.ZH to "安装", Language.EN to "Install"
    )
    val button_title_open = SimpleI18nResource(
      Language.ZH to "打开", Language.EN to "Open"
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

    val tip_empty = SimpleI18nResource(
      Language.ZH to "暂无下载任务和记录", Language.EN to "No Download Tasks And Records"
    )

    val dropdown_delete = SimpleI18nResource(Language.ZH to "删除", Language.EN to "Delete")
    val dropdown_share = SimpleI18nResource(Language.ZH to "分享", Language.EN to "Share")
    val dropdown_rename = SimpleI18nResource(Language.ZH to "重命名", Language.EN to "Rename")

    val chip_all = SimpleI18nResource(Language.ZH to "所有", Language.EN to "All")
    val chip_image = SimpleI18nResource(Language.ZH to "图片", Language.EN to "Photo")
    val chip_video = SimpleI18nResource(Language.ZH to "视频", Language.EN to "Video")
    val chip_audio = SimpleI18nResource(Language.ZH to "音频", Language.EN to "Audio")
    val chip_doc = SimpleI18nResource(Language.ZH to "文档", Language.EN to "Documents")
    val chip_package = SimpleI18nResource(Language.ZH to "压缩包", Language.EN to "Archives")
    val chip_other = SimpleI18nResource(Language.ZH to "其它", Language.EN to "Other")
  }

  object Setting {
    val page_title = SimpleI18nResource(Language.ZH to "设置", Language.EN to "Setting")
  }

  object QRCode {
    val short_name = SimpleI18nResource(Language.ZH to "智能扫码", Language.EN to "Smart Scan")
    val select_QR_code = SimpleI18nResource(
      Language.ZH to "选择一张二维码图片",
      Language.EN to "Select a QR code image"
    )
    val toast_mismatching = SimpleI18nResource(
      Language.ZH to "无法解析的数据 -> %s",
      Language.EN to "no support data -> %s"
    )
    val permission_tip_camera_title = SimpleI18nResource(
      Language.ZH to "授权相机权限",
      Language.EN to "Authorize camera permissions"
    )
    val permission_tip_camera_message = SimpleI18nResource(
      Language.ZH to "DwebBrowser正在向您获取“相机”权限，同意后，将用于为您提供扫描二维码服务",
      Language.EN to "DwebBrowser is asking you for \"Camera\" permissions, and if you agree, it will be used to provide you with scanning QR code services"
    )

    val permission_denied = SimpleI18nResource(
      Language.ZH to "获取“相机”权限失败，无法提供扫码服务",
      Language.EN to "Failed to obtain the Camera permission and cannot provide the code scanning service"
    )

    val noFoundWindow = SimpleI18nResource(
      Language.ZH to "无法找到承载的窗口信息",
      Language.EN to "Failed to obtain the Camera permission and cannot provide the code scanning service"
    )

    val Action = SimpleI18nResource(
      Language.ZH to "打开",
      Language.EN to "Open"
    )

    val emptyResult = SimpleI18nResource(
      Language.ZH to "没有识别到条形码",
      Language.EN to "No barcode recognized"
    )

    val confirm = SimpleI18nResource(
      Language.ZH to "确定",
      Language.EN to "Confirm"
    )
    val dismiss = SimpleI18nResource(
      Language.ZH to "关闭",
      Language.EN to "Dismiss"
    )
    val Back = SimpleI18nResource(
      Language.ZH to "返回",
      Language.EN to "Back"
    )
    val photo_album = SimpleI18nResource(
      Language.ZH to "相册",
      Language.EN to "Album"
    )
    val photo_endoscopic = SimpleI18nResource(
      Language.ZH to "内窥",
      Language.EN to "Endoscopic"
    )
    val simulator_title = SimpleI18nResource(
      Language.ZH to "在模拟器上不可用",
      Language.EN to "not available on simulator"
    )
    val simulator_body = SimpleI18nResource(
      Language.ZH to """
        相机在模拟器上不可用。
        请尝试在真正的iOS设备上运行。
      """.trimIndent(),
      Language.EN to """
      Camera is not available on simulator.
      Please try to run on a real iOS device.
      """
    )
    val webcam_detected_title = SimpleI18nResource(
      Language.ZH to "没有检测到摄像头",
      Language.EN to "Camera not detected"
    )
    val webcam_detected_body = SimpleI18nResource(
      Language.ZH to "是否切换为内窥模式？",
      Language.EN to "Switch to Endoscopic Mode?"
    )
    val tip_no_camera = SimpleI18nResource(
      Language.ZH to "没有检测到摄像头，请连接摄像头或者通过文件选择器选择图片",
      Language.EN to "No camera detected, please connect camera or use file selector"
    )
  }

  object IconDescription {
    val verified = SimpleI18nResource(
      Language.ZH to "已认证",
      Language.EN to "Verified"
    )

    val unverified = SimpleI18nResource(
      Language.ZH to "未认证",
      Language.EN to "Unverified"
    )
  }

  object Desk {
    val short_name = SimpleI18nResource(
      Language.ZH to "我的桌面",
      Language.EN to "My Desk"
    )
  }

  object Desktop {
    val quit = SimpleI18nResource(
      Language.ZH to "退出",
      Language.EN to "Close"
    )

    val detail = SimpleI18nResource(
      Language.ZH to "应用详情",
      Language.EN to "App Info"
    )

    val delete = SimpleI18nResource(
      Language.ZH to "删除",
      Language.EN to "Delete"
    )

    val share = SimpleI18nResource(
      Language.ZH to "分享",
      Language.EN to "Share"
    )
  }
}