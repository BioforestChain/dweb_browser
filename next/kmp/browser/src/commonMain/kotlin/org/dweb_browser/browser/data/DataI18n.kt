package org.dweb_browser.browser.data

import org.dweb_browser.helper.compose.I18n

object DataI18n : I18n() {
  val short_name = zh("数据管理", "Store Manager")

  val uninstalled = zh("应用已卸载", "The application has been uninstalled")
  val uninstall_running_app_title = zh("程序正在运行", "The application is running")
  val uninstall_running_app_tip =
    zh1({ "$value 正在运行，如果要清除它的数据，将会先关停应用，然后执行数据清理工作。" },
      { "$value is currently running. If you want to clear its data, the application will first be shut down, and then the data cleanup process will be executed." })
  val uninstall_app_tip = zh1({ "确定要清除 $value 的应用数据吗？" },
    { "Confirm to clear the application data for $value?" })

  val select_profile_for_detail_view =
    zh("请选择一项数据", "Please select one item to view the details")
  val no_support_detail_view =
    zh("暂时不支持数据详情的查看", "The data details view is not supported at present")
}