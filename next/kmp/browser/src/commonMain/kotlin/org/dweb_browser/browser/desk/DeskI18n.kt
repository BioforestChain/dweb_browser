package org.dweb_browser.browser.desk

import org.dweb_browser.helper.compose.I18n

object DeskI18n : I18n() {
  val uninstall = zh("卸载", "Uninstall")

  //  val delete_weblink_title =
//    zh1({ "确定要删除网页链接：《$value》" }, { "Confirm to delete the web link: '$value'" })
//  val delete_app_title = zh1({ "确定要卸载应用：$value" }, { "Confirm to uninstall app: $value" })
  val delete_weblink_title = zh("确定要删除网页链接", "Confirm to delete the web link")
  val delete_app_title = zh("确定要卸载应用", "Confirm to uninstall the app")
  val delete_app_tip = zh("其所有数据也将被删除", "All its data will also be deleted!")
}