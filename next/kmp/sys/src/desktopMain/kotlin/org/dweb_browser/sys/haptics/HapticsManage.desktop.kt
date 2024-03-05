package org.dweb_browser.sys.haptics

/**
 * TODO MacOS 和 IOS 共享基于 CoreHaptics 的实现
 *
 * > Windows 目前没有相关支持，[参考 MAUI 的 uwp 实现](https://github.com/dotnet/maui/blob/main/src/Essentials/src/Vibration/Vibration.uwp.cs)
 */
actual class VibrateManage actual constructor() {
  actual fun vibratePre26(pattern: LongArray, repeat: Int) {
  }

  actual fun impact(type: HapticsImpactType) {
  }

  actual fun notification(type: HapticsNotificationType) {
  }

  actual fun vibrateClick() {
  }

  actual fun vibrateDisabled() {
  }

  actual fun vibrateDoubleClick() {
  }

  actual fun vibrateHeavyClick() {
  }

  actual fun vibrateTick() {
  }

}