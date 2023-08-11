package info.bagen.dwebbrowser.base


import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.RoundedCorner
import android.view.WindowInsets
import androidx.annotation.RequiresApi

class WindowInsetsHelper {
  companion object {

    /**
     * miui 开发者文档 https://dev.mi.com/distribute/doc/details?pId=1631
     */
    @SuppressLint("DiscouragedApi")
    fun getCornerRadiusTop(context: Context, density: Float, defaultValue: Float): Float {
      var radius = defaultValue;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        WindowInsets.CONSUMED.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.also {
          radius = it.radius / density
        }
      } else {
        val resourceId = context.resources.getIdentifier(
          "rounded_corner_radius_top", "dimen", "android"
        );
        if (resourceId > 0) {
          radius = context.resources.getDimensionPixelSize(resourceId) / density
        }
      }
      return radius;
    }

    @SuppressLint("DiscouragedApi")
    fun getCornerRadiusBottom(context: Context, density: Float, defaultValue: Float): Float {
      var radius = defaultValue;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        WindowInsets.CONSUMED.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.also {
          radius = it.radius / density
        }
      } else {
        val resourceId = context.resources.getIdentifier(
          "rounded_corner_radius_bottom", "dimen", "android"
        );
        if (resourceId > 0) {
          radius = context.resources.getDimensionPixelSize(resourceId) / density
        }
      }
      return radius;
    }
  }
}