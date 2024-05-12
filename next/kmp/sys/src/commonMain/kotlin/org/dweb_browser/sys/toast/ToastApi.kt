package org.dweb_browser.sys.toast

import org.dweb_browser.core.module.MicroModule

expect suspend fun showToast(
  microModule: MicroModule.Runtime,
  text: String,
  durationType: ToastDurationType = ToastDurationType.SHORT,
  positionType: ToastPositionType = ToastPositionType.BOTTOM
)