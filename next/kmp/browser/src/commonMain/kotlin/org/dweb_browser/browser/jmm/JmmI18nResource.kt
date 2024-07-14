package org.dweb_browser.browser.jmm

import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

object JmmI18nResource {
  val top_bar_title_install =
    SimpleI18nResource(Language.ZH to "应用列表", Language.EN to "Application Manage")
  val no_select_detail = SimpleI18nResource(
    Language.ZH to "未选择要展示的详情",
    Language.EN to "select item to show details",
  )

  val tab_detail = SimpleI18nResource(Language.ZH to "详情", Language.EN to "Detail")
  val tab_intro = SimpleI18nResource(Language.ZH to "介绍", Language.EN to "Introduction")
  val tab_param = SimpleI18nResource(Language.ZH to "参数", Language.EN to "Parameter")

  val short_name = SimpleI18nResource(Language.ZH to "安装管理", Language.EN to "Install Manager")
  val history_tab_installed =
    SimpleI18nResource(Language.ZH to "已安装", Language.EN to "Installed")
  val history_tab_uninstalled =
    SimpleI18nResource(Language.ZH to "未安装", Language.EN to "No Install")
  val install_mmid = SimpleI18nResource(Language.ZH to "唯一标识", Language.EN to "id")
  val install_version = SimpleI18nResource(Language.ZH to "版本", Language.EN to "version")
  val install_introduction =
    SimpleI18nResource(Language.ZH to "应用介绍", Language.EN to "Introduction")
  val install_update_log =
    SimpleI18nResource(Language.ZH to "更新日志", Language.EN to "Update Log")
  val install_info = SimpleI18nResource(Language.ZH to "信息", Language.EN to "Info")
  val install_info_dev =
    SimpleI18nResource(Language.ZH to "开发者", Language.EN to "Developer")
  val install_info_size = SimpleI18nResource(Language.ZH to "应用大小", Language.EN to "App Size")
  val install_info_type = SimpleI18nResource(Language.ZH to "类别", Language.EN to "Type")
  val install_info_language =
    SimpleI18nResource(Language.ZH to "语言", Language.EN to "Language")
  val install_info_age = SimpleI18nResource(Language.ZH to "年龄", Language.EN to "Age")
  val install_info_copyright =
    SimpleI18nResource(Language.ZH to "版权", Language.EN to "CopyRight")
  val install_info_homepage =
    SimpleI18nResource(Language.ZH to "应用主页", Language.EN to "Home Page")
  val history_details = SimpleI18nResource(Language.ZH to "详情", Language.EN to "Details")
  val history_uninstall =
    SimpleI18nResource(Language.ZH to "卸载", Language.EN to "Uninstall")
  val url_invalid =
    SimpleI18nResource(
      Language.ZH to "网址已失效，请前往官网进行安装！",
      Language.EN to "The website is no longer valid, please go to the official website to install!"
    )
}