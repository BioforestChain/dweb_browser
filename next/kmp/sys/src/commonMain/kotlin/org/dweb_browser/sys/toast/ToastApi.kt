package org.dweb_browser.sys.toast

import org.dweb_browser.core.module.MicroModule

expect suspend fun showToast(
  microModule: MicroModule,
  text: String,
  durationType: DurationType = DurationType.SHORT,
  positionType: PositionType = PositionType.BOTTOM
)