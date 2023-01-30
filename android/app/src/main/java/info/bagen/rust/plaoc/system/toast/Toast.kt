package info.bagen.rust.plaoc.system.toast

import android.view.Gravity
import android.view.View
import info.bagen.rust.plaoc.App

object Toast {

    enum class DurationType(duration: Long) {
        SHORT(2000L), LONG(3500L)
    }

    enum class PositionType(position: String) {
        TOP("top"), CENTER("center"), BOTTOM("bottom")
    }

    fun show(
        text: String, view: View?,
        durationType: DurationType = DurationType.SHORT,
        positionType: PositionType = PositionType.BOTTOM
    ) {
        showToast(text, durationType, positionType)
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

    /**
     * 由于 SetGravity 的功能在 Build.VERSION_CODES.R 及其以上版本已经无法使用，所以这边需要改为 SnackBar
     */
    private fun showToast(
        text: String,
        durationType: DurationType = DurationType.SHORT,
        positionType: PositionType = PositionType.BOTTOM
    ) {
        val duration = when (durationType) {
            DurationType.LONG -> android.widget.Toast.LENGTH_LONG
            else -> android.widget.Toast.LENGTH_SHORT
        }
        val toast = android.widget.Toast.makeText(App.appContext, text, duration)
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

data class ToastOption(
    val text: String = "",
    val duration: String = "short",
    val position: String = "bottom"
)
