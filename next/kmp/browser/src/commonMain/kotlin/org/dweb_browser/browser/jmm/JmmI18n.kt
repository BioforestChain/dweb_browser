package org.dweb_browser.browser.jmm

import org.dweb_browser.helper.compose.I18n

object JmmI18n : I18n() {
  val uninstall_alert_title = zh("确定要卸载应用？", "Confirm to uninstall?")
  val confirm_uninstall = zh("卸载", "Uninstall")
  val prepare_install = zh("正在检查应用信息", "Preparing to install")
  val prepare_install_ready = zh("应用信息准备完毕", "Ready for installation")
  val prepare_install_fail = zh("应用信息异常", "Installation unavailable")
}