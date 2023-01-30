package info.bagen.rust.plaoc.system.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import info.bagen.rust.plaoc.App

enum class HapticsNotificationType(
    val type: String, val timings: LongArray, val amplitudes: IntArray, val oldSDKPattern: LongArray
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
        "ERROR",
        longArrayOf(0, 27, 45, 50),
        intArrayOf(0, 120, 0, 25),
        longArrayOf(0, 27, 45, 50)
    ),
}

enum class HapticsImpactType(
    val type: String, val timings: LongArray, val amplitudes: IntArray, val oldSDKPattern: LongArray
) {
    LIGHT("LIGHT", longArrayOf(0, 50), intArrayOf(0, 110), longArrayOf(0, 20)),
    MEDIUM(
        "MEDIUM", longArrayOf(0, 43), intArrayOf(0, 180), longArrayOf(0, 43)
    ),
    HEAVY("HEAVY", longArrayOf(0, 60), intArrayOf(0, 255), longArrayOf(0, 61)),
}

enum class VibrateType(
    val type: String, val timings: LongArray, val amplitudes: IntArray, val oldSDKPattern: LongArray
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
    TICK("TICK", longArrayOf(0, 10), intArrayOf(0, 10), longArrayOf(10, 999, 1, 1)),
    DISABLED(
        "DISABLED", longArrayOf(0, 10), intArrayOf(0, 10), longArrayOf(1, 63, 1, 119, 1, 129, 1)
    ),
}

class VibrateManage() {
    private var mVibrate: Vibrator

    init {
        mVibrate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm =
                App.appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            getDeprecatedVibrator(App.appContext)
        }
    }

    @SuppressWarnings("deprecation")
    private fun getDeprecatedVibrator(context: Context): Vibrator {
        return context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    @SuppressWarnings("deprecation")
    private fun vibratePre26(duration: Long) {
        mVibrate.vibrate(duration)
    }

    @SuppressWarnings("deprecation")
    private fun vibratePre26(pattern: LongArray, repeat: Int) {
        mVibrate.vibrate(pattern, repeat)
    }

    companion object {
        val sInstance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            VibrateManage()
        }
    }

    fun vibrate(duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mVibrate.vibrate(
                VibrationEffect.createOneShot(
                    duration,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibratePre26(duration)
        }
    }

    /**
     * 触碰轻质量物体
     */
    fun impact(type: HapticsImpactType = HapticsImpactType.HEAVY) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mVibrate.vibrate(VibrationEffect.createWaveform(type.timings, type.amplitudes, -1))
        } else {
            vibratePre26(type.oldSDKPattern, -1)
        }
    }

    /**
     * 警告分隔的振动通知
     */
    fun notification(type: HapticsNotificationType = HapticsNotificationType.WARNING) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mVibrate.vibrate(VibrationEffect.createWaveform(type.timings, type.amplitudes, -1))
        } else {
            vibratePre26(type.oldSDKPattern, -1)
        }
    }

    /**
     * 单击手势的反馈振动
     */
    fun vibrateClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mVibrate.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            vibratePre26(VibrateType.CLICK.oldSDKPattern, -1)
        }
    }

    /**
     * 禁用手势的反馈振动，与headShak特效一致
     */
    fun vibrateDisabled() {
        // mVibrate.cancel()
        vibratePre26(VibrateType.DISABLED.oldSDKPattern, -1)
    }

    /**
     * 双击手势的反馈振动
     */
    fun vibrateDoubleClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mVibrate.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        } else {
            vibratePre26(VibrateType.DOUBLE_CLICK.oldSDKPattern, -1)
        }
    }

    /**
     * 重击手势的反馈振动，比如菜单键/长按/3DTouch
     */
    fun vibrateHeavyClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mVibrate.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            vibratePre26(VibrateType.HEAVY_CLICK.oldSDKPattern, -1)
        }
    }

    /**
     * 滴答
     */
    fun vibrateTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mVibrate.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            vibratePre26(VibrateType.TICK.oldSDKPattern, -1)
        }
    }
}

data class ImpactOption(
    val style: String = "LIGHT"
)

data class NotificationOption(
    val type: String = "Warning"
)

data class VibrateOption(
    val duration: Long = 1
)
