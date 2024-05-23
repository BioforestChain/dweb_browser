package org.dweb_browser.sys.toast

import android.view.Gravity
import android.widget.Toast
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.getAppContextUnsafe
import org.dweb_browser.helper.withMainContext

actual suspend fun showToast(
  microModule: MicroModule.Runtime,
  text: String, durationType: ToastDurationType, positionType: ToastPositionType
) =
  ToastController.showToast(text, durationType, positionType)

object ToastController {
  /**
   * 由于 SetGravity 的功能在 Build.VERSION_CODES.R 及其以上版本已经无法使用，所以这边需要改为 SnackBar
   */
  suspend fun showToast(
    text: String,
    durationType: ToastDurationType,
    positionType: ToastPositionType
  ) {
    val duration = when (durationType) {
      ToastDurationType.LONG -> Toast.LENGTH_LONG
      else -> Toast.LENGTH_SHORT
    }
    withMainContext {
      Toast.makeText(getAppContextUnsafe(), text, duration).also {
        when (positionType) {
          ToastPositionType.TOP -> {
            it.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 40)
          }

          ToastPositionType.CENTER -> {
            it.setGravity(Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL, 0, 0)
          }

          else -> {}
        }
      }.show()
    }
  }
}