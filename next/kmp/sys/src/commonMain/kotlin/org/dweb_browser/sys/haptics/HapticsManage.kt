package org.dweb_browser.sys.haptics

enum class HapticsNotificationType(
  val type: String, val timings: LongArray, val amplitudes: IntArray, val oldSDKPattern: LongArray,
) {
  SUCCESS(
    "SUCCESS",
    longArrayOf(0, 35, 65, 21),
    intArrayOf(0, 250, 0, 180),
    longArrayOf(0, 35, 65, 21)
  ),
  WARNING(
    "WARNING",
    longArrayOf(0, 30, 40, 30, 50, 60),
    intArrayOf(255, 255, 255, 255, 255, 255),
    longArrayOf(0, 30, 40, 30, 50, 60)
  ),
  ERROR(
    "ERROR", longArrayOf(0, 27, 45, 50), intArrayOf(0, 120, 0, 25), longArrayOf(0, 27, 45, 50)
  ),
  ;

  companion object {
    val ALL = entries.associateBy { it.type }
  }
}

enum class HapticsImpactType(
  val type: String, val milliseconds: Long, val amplitude: Int,
) {
  LIGHT("LIGHT", 5, 64),
  MEDIUM("MEDIUM", 10, 128),
  HEAVY("HEAVY", 20, 255),
  ;

  companion object {
    val ALL = entries.associateBy { it.type }
  }
}

enum class VibrateType(
  val type: String, val timings: LongArray, val amplitudes: IntArray, val oldSDKPattern: LongArray,
) {
  CLICK(
    "CLICK", longArrayOf(0, 10), intArrayOf(0, 10), longArrayOf(1)
  ),
  DOUBLE_CLICK(
    "DOUBLE_CLICK", longArrayOf(0, 10), intArrayOf(0, 10), longArrayOf(10, 1)
  ),
  HEAVY_CLICK(
    "HEAVY_CLICK", longArrayOf(0, 10), intArrayOf(0, 10), longArrayOf(1, 100, 1, 1)
  ),
  TICK("TICK", longArrayOf(0, 10), intArrayOf(0, 10), longArrayOf(10, 999, 1, 1)), DISABLED(
    "DISABLED", longArrayOf(0, 10), intArrayOf(0, 10), longArrayOf(1, 63, 1, 119, 1, 129, 1)
  ),
}

expect class VibrateManage() {

  fun vibratePre26(pattern: LongArray, repeat: Int)
  fun impact(type: HapticsImpactType)

  fun notification(type: HapticsNotificationType)

  fun vibrateClick()

  fun vibrateDisabled()

  fun vibrateDoubleClick()

  fun vibrateHeavyClick()

  fun vibrateTick()
}

data class ImpactOption(
  val style: String = "LIGHT",
)

data class NotificationOption(
  val type: String = "Warning",
)

data class VibrateOption(
  val duration: Long = 1,
)