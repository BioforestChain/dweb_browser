package org.dweb_browser.sys.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import org.dweb_browser.helper.getAppContextUnsafe

actual class VibrateManage actual constructor() {

  private var mVibrate: Vibrator
  private val context = getAppContextUnsafe()

  init {
    mVibrate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vm =
        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
      vm.defaultVibrator
    } else {
      getDeprecatedVibrator(context)
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
  public actual fun vibratePre26(pattern: LongArray, repeat: Int) {
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
          duration, VibrationEffect.DEFAULT_AMPLITUDE
        )
      )
    } else {
      vibratePre26(duration)
    }
  }

  /**
   * 触碰轻质量物体
   */
  actual fun impact(type: HapticsImpactType) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      mVibrate.vibrate(VibrationEffect.createWaveform(type.timings, type.amplitudes, -1))
    } else {
      vibratePre26(type.oldSDKPattern, -1)
    }
  }

  /**
   * 警告分隔的振动通知
   */
  actual fun notification(type: HapticsNotificationType) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      mVibrate.vibrate(VibrationEffect.createWaveform(type.timings, type.amplitudes, -1))
    } else {
      vibratePre26(type.oldSDKPattern, -1)
    }
  }

  /**
   * 单击手势的反馈振动
   */
  actual fun vibrateClick() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      mVibrate.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    } else {
      vibratePre26(VibrateType.CLICK.oldSDKPattern, -1)
    }
  }

  /**
   * 禁用手势的反馈振动，与headShak特效一致
   */
  actual fun vibrateDisabled() {
    // mVibrate.cancel()
    vibratePre26(VibrateType.DISABLED.oldSDKPattern, -1)
  }

  /**
   * 双击手势的反馈振动
   */
  actual fun vibrateDoubleClick() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      mVibrate.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
    } else {
      vibratePre26(VibrateType.DOUBLE_CLICK.oldSDKPattern, -1)
    }
  }

  /**
   * 重击手势的反馈振动，比如菜单键/长按/3DTouch
   */
  actual fun vibrateHeavyClick() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      mVibrate.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
    } else {
      vibratePre26(VibrateType.HEAVY_CLICK.oldSDKPattern, -1)
    }
  }

  /**
   * 滴答
   */
  actual fun vibrateTick() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      mVibrate.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    } else {
      vibratePre26(VibrateType.TICK.oldSDKPattern, -1)
    }
  }
}