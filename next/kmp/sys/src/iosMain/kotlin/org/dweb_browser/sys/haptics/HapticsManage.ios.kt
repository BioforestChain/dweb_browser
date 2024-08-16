package org.dweb_browser.sys.haptics

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGFloat
import platform.CoreHaptics.CHHapticDynamicParameter
import platform.CoreHaptics.CHHapticEngine
import platform.CoreHaptics.CHHapticEvent
import platform.CoreHaptics.CHHapticEventParameter
import platform.CoreHaptics.CHHapticEventParameterIDHapticIntensity
import platform.CoreHaptics.CHHapticEventParameterIDHapticSharpness
import platform.CoreHaptics.CHHapticEventTypeHapticContinuous
import platform.CoreHaptics.CHHapticPattern
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import kotlin.math.max

actual class VibrateManage actual constructor() {

  private val capabilities = CHHapticEngine.capabilitiesForHardware()

  actual fun vibratePre26(pattern: LongArray, repeat: Int) {
    val durationArr = pattern.toMutableList()
    startVibrate(durationArr)

  }

  /**
   * 触碰轻质量物体
   */
  actual fun impact(type: HapticsImpactType) {
    impactVibrate(type)
  }

  /**
   * 警告分隔的振动通知
   */
  actual fun notification(type: HapticsNotificationType) {
    notificationVibrate(type)
  }

  /**
   * 单击手势的反馈振动
   */
  actual fun vibrateClick() {
    vibrateAction(VibrateType.CLICK)
  }

  /**
   * 禁用手势的反馈振动，与headShak特效一致
   */
  actual fun vibrateDisabled() {
    vibrateAction(VibrateType.DISABLED)
  }

  /**
   * 双击手势的反馈振动
   */
  actual fun vibrateDoubleClick() {
    vibrateAction(VibrateType.DOUBLE_CLICK)
  }

  /**
   * 重击手势的反馈振动，比如菜单键/长按/3DTouch
   */
  actual fun vibrateHeavyClick() {
    vibrateAction(VibrateType.HEAVY_CLICK)
  }

  /**
   * 滴答
   */
  actual fun vibrateTick() {
    vibrateAction(VibrateType.TICK)
  }

  private fun impactVibrate(style: HapticsImpactType) {

    val impactType = when (style) {
      HapticsImpactType.MEDIUM -> UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium
      HapticsImpactType.HEAVY -> UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy
      else -> UIImpactFeedbackStyle.UIImpactFeedbackStyleLight
    }

    val impact = UIImpactFeedbackGenerator(impactType)
    impact.prepare()
    impact.impactOccurred()
  }

  private fun notificationVibrate(style: HapticsNotificationType) {
    val notiType = when (style) {
      HapticsNotificationType.SUCCESS -> UINotificationFeedbackType.UINotificationFeedbackTypeSuccess
      HapticsNotificationType.WARNING -> UINotificationFeedbackType.UINotificationFeedbackTypeWarning
      else -> UINotificationFeedbackType.UINotificationFeedbackTypeError
    }

    val notification = UINotificationFeedbackGenerator()
    notification.prepare()
    notification.notificationOccurred(notiType)
  }

  private fun vibrateAction(style: VibrateType) {
    if (!capabilities.supportsHaptics) {
      return
    }
    val durationArr: MutableList<Long> = when (style) {
      VibrateType.CLICK -> mutableListOf(1)
      VibrateType.DISABLED -> mutableListOf(1, 63, 1, 119, 1, 129, 1)
      VibrateType.DOUBLE_CLICK -> mutableListOf(10, 1)
      VibrateType.HEAVY_CLICK -> mutableListOf(1, 100, 1, 1)
      VibrateType.TICK -> mutableListOf(10, 999, 1, 1)
      else -> mutableListOf()
    }
    startVibrate(durationArr)
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun startVibrate(durationArr: MutableList<Long>) {

    val engine = CHHapticEngine(null)
    engine.startAndReturnError(null)

    val events = mutableListOf<CHHapticEvent>()
    var relativeTime: CGFloat = 0.0

    durationArr.forEachIndexed { index, duration ->
      if (index % 2 == 0) {
        val intensity =
          CHHapticEventParameter(CHHapticEventParameterIDHapticIntensity, value = 0.5f)
        val sharpness =
          CHHapticEventParameter(CHHapticEventParameterIDHapticSharpness, value = 0.6f)
        val parameters = listOf<CHHapticEventParameter>(intensity, sharpness)
        val continuousEvent = CHHapticEvent(
          CHHapticEventTypeHapticContinuous,
          parameters = parameters,
          relativeTime,
          duration = max(0.01, duration.toDouble() / 1000)
        )
        events.add(continuousEvent)
      }
      relativeTime += duration.toDouble() / 1000
    }
    val parame = emptyList<CHHapticDynamicParameter>()
    val pattern = CHHapticPattern(events = events, parameters = parame, error = null)
    val player = engine.createPlayerWithPattern(pattern, null)
    player?.startAtTime(0.0, null)
  }
}