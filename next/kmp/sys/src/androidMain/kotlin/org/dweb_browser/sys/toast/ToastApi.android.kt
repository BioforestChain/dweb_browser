package org.dweb_browser.sys.toast

import android.view.Gravity
import android.view.View
import android.widget.Toast
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.withMainContext

actual suspend fun showToast(
  microModule: MicroModule.Runtime,
  text: String, durationType: DurationType, positionType: PositionType
) =
  ToastController.showToast(text, durationType, positionType)

object ToastController {
  private fun showSnackBar(
    text: String, view: View,
    durationType: DurationType = DurationType.SHORT,
    positionType: PositionType = PositionType.BOTTOM
  ) {
    com.google.android.material.snackbar.Snackbar.make(
      view, text, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
    ).show()
  }

  /**
   * 由于 SetGravity 的功能在 Build.VERSION_CODES.R 及其以上版本已经无法使用，所以这边需要改为 SnackBar
   */
  suspend fun showToast(text: String, durationType: DurationType, positionType: PositionType) {
    val duration = when (durationType) {
      DurationType.LONG -> Toast.LENGTH_LONG
      else -> Toast.LENGTH_SHORT
    }
    withMainContext {
      Toast.makeText(getAppContext(), text, duration).also {
        when (positionType) {
          PositionType.TOP -> {
            it.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 40)
          }

          PositionType.CENTER -> {
            it.setGravity(Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL, 0, 0)
          }

          else -> {}
        }
      }.show()
    }
  }
}