package org.dweb_browser.shared.microService.sys.toast

import android.view.Gravity
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext


actual object ToastController {

    val context get() = NativeMicroModule.getAppContext()

    actual fun show(
        text: String,
        durationType: DurationType,
        positionType: PositionType
    ) {
        showToast(text, durationType, positionType)
    }

    private fun showToast(
        text: String,
        durationType: DurationType = DurationType.SHORT,
        positionType: PositionType = PositionType.BOTTOM
    ) {
        val duration = when (durationType) {
            DurationType.LONG -> android.widget.Toast.LENGTH_LONG
            else -> android.widget.Toast.LENGTH_SHORT
        }
        runBlocking(Dispatchers.Main) {
            val toast = Toast.makeText(context, text, duration)
            when (positionType) {
                PositionType.TOP -> {
                    toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 40)
                }

                PositionType.CENTER -> {
                    toast.setGravity(Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL, 0, 0)
                }

                else -> {}
            }
            toast.show()
        }
    }

    private fun showSnackBar(
        text: String, view: View,
        durationType: DurationType = DurationType.SHORT,
        positionType: PositionType = PositionType.BOTTOM
    ) {
        com.google.android.material.snackbar.Snackbar.make(
            view, text, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }
}

data class ToastOption(
    val text: String = "",
    val duration: String = "short",
    val position: String = "bottom"
)