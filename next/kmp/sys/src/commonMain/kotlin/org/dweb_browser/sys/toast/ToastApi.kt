package org.dweb_browser.sys.toast

expect suspend fun showToast(
  text: String,
  durationType: DurationType = DurationType.SHORT,
  positionType: PositionType = PositionType.BOTTOM
)