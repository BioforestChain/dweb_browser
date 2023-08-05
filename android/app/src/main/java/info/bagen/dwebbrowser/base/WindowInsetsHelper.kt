package info.bagen.dwebbrowser.base


import android.annotation.SuppressLint
import android.content.Context

class WindowInsetsHelper {
  companion object {

    @SuppressLint("DiscouragedApi")
    fun getCornerRadiusTop(context: Context, density: Float, defaultValue: Float): Float {
      var radius = defaultValue;
      val resourceId = context.resources.getIdentifier(
        "rounded_corner_radius_top", "dimen", "android"
      );
      if (resourceId > 0) {
        radius = context.resources.getDimensionPixelSize(resourceId) / density
      }

      return radius;
    }

    @SuppressLint("DiscouragedApi")
    fun getCornerRadiusBottom(context: Context, density: Float, defaultValue: Float): Float {
      var radius = defaultValue;
      val resourceId = context.resources.getIdentifier(
        "rounded_corner_radius_bottom", "dimen", "android"
      );
      if (resourceId > 0) {
        radius = context.resources.getDimensionPixelSize(resourceId) / density
      }
      return radius;
    }
  }
}