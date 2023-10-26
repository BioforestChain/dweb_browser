package org.dweb_browser.shared.microService.sys.haptics

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
        val durationArr = pattern.toTypedArray()
        startVibrate(durationArr)
    }
    actual fun impact(type: HapticsImpactType) {
        impactVibrate(type)
    }

    actual fun notification(type: HapticsNotificationType) {
        notificationVibrate(type)
    }

    actual fun vibrateClick() {
        vibrateAction(VibrateType.CLICK)
    }

    actual fun vibrateDisabled() {
        vibrateAction(VibrateType.DISABLED)
    }

    actual fun vibrateDoubleClick() {
        vibrateAction(VibrateType.DOUBLE_CLICK)
    }

    actual fun vibrateHeavyClick() {
        vibrateAction(VibrateType.HEAVY_CLICK)
    }

    actual fun vibrateTick() {
        vibrateAction(VibrateType.TICK)
    }

    private fun impactVibrate(style: HapticsImpactType) {

        var impactType = when(style) {
            HapticsImpactType.MEDIUM -> UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium
            HapticsImpactType.HEAVY -> UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy
            else -> UIImpactFeedbackStyle.UIImpactFeedbackStyleLight
        }

        var impact = UIImpactFeedbackGenerator(impactType)
        impact.prepare()
        impact.impactOccurred()
    }

    private fun notificationVibrate(style: HapticsNotificationType) {
        var notiType = when(style) {
            HapticsNotificationType.SUCCESS -> UINotificationFeedbackType.UINotificationFeedbackTypeSuccess
            HapticsNotificationType.WARNING -> UINotificationFeedbackType.UINotificationFeedbackTypeWarning
            else -> UINotificationFeedbackType.UINotificationFeedbackTypeError
        }

        var notification = UINotificationFeedbackGenerator()
        notification.prepare()
        notification.notificationOccurred(notiType)
    }

    private fun vibrateAction(style: VibrateType) {
        if (!capabilities.supportsHaptics) {
            return
        }
        var durationArr: Array<Long> = emptyArray()
        when(style) {
            VibrateType.CLICK -> durationArr = arrayOf(1)
            VibrateType.DISABLED -> durationArr = arrayOf(1, 63, 1, 119, 1, 129, 1)
            VibrateType.DOUBLE_CLICK -> durationArr = arrayOf(10, 1)
            VibrateType.HEAVY_CLICK -> durationArr = arrayOf(1, 100, 1, 1)
            VibrateType.TICK -> durationArr = arrayOf(10, 999, 1, 1)
            else -> durationArr = emptyArray()
        }

        startVibrate(durationArr)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun startVibrate(durationArr: Array<Long>) {

        val engine = CHHapticEngine()
        engine.startWithCompletionHandler {
            var events: List<CHHapticEvent> = emptyList()
            var relativeTime: CGFloat = 0.0

            durationArr.forEachIndexed { index, duration ->
                if (index % 2 == 0) {
                    val intensity = CHHapticEventParameter(CHHapticEventParameterIDHapticIntensity, value = 0.5f)
                    val sharpness = CHHapticEventParameter(CHHapticEventParameterIDHapticSharpness, value = 0.6f)
                    val parameters = listOf<CHHapticEventParameter>(intensity,sharpness)
                    val continuousEvent = CHHapticEvent(CHHapticEventTypeHapticContinuous, parameters = parameters, relativeTime, duration = max(0.01,duration.toDouble() / 1000))
                    events.plus(continuousEvent)
                }
                relativeTime += duration.toDouble() / 1000
            }
            val parame = emptyList<CHHapticDynamicParameter>()
            val pattern = CHHapticPattern(events = events, parameters = parame, error = null)
            val player = engine.createPlayerWithPattern(pattern,null)
            player?.startAtTime(0.0, null)
        }
    }
}