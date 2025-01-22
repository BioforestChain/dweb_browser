package org.dweb_browser.helper.android

/**
 * 参考资料
 * link: https://github.com/Tencent/Hippy/blob/1aa96f16e2769cd372f848ddc66603f7cf2571b0/modules/android/hippy_support/src/main/java/com/tencent/mtt/hippy/utils/DimensionsUtil.java
 */
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.provider.Settings
import android.text.TextUtils

/// android 各手机平台虚拟导航键盘处理
object NavigationBarUtil {
  private const val NAV_BAR_HEIGHT_RES_NAME = "navigation_bar_height"
  private const val NAV_BAR_HEIGHT_LANDSCAPE_RES_NAME = "navigation_bar_height_landscape"
  private const val SHOW_NAV_BAR_RES_NAME = "config_showNavigationBar"

  private val navigationBarIsMinKeyName: String
    get() {
      val brand = Build.BRAND
      if (TextUtils.isEmpty(brand)) {
        return "navigationbar_is_min"
      }

      return if (brand.equals("HUAWEI", ignoreCase = true)) {
        "navigationbar_is_min"
      } else if (brand.equals("XIAOMI", ignoreCase = true)) {
        "force_fsg_nav_bar"
      } else if (brand.equals("VIVO", ignoreCase = true)) {
        "navigation_gesture_on"
      } else if (brand.equals("OPPO", ignoreCase = true)) {
        "navigation_gesture_on"
      } else {
        "navigationbar_is_min"
      }
    }

  @SuppressLint("PrivateApi", "DiscouragedApi")
  private fun checkNavigationBarShow(context: Context): Boolean {
    var checkResult = false
    val rs = context.resources
    val id = rs.getIdentifier(SHOW_NAV_BAR_RES_NAME, "bool", "android")
    if (id > 0) {
      checkResult = rs.getBoolean(id)
    }
    try {
      val systemPropertiesClass = Class.forName("android.os.SystemProperties")
      val m = systemPropertiesClass.getMethod("get", String::class.java)
      val navBarOverride = m.invoke(systemPropertiesClass, "qemu.hw.mainkeys") as String
      //判断是否隐藏了底部虚拟导航
      val navigationBarIsMin = Settings.Global.getInt(
        context.contentResolver, navigationBarIsMinKeyName, 0
      )
      if ("1" == navBarOverride || 1 == navigationBarIsMin) {
        checkResult = false
      } else if ("0" == navBarOverride) {
        checkResult = true
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    return checkResult
  }

  /**
   * 获取虚拟按键的高度 1. 全面屏下 1.1 开启全面屏开关-返回0 1.2 关闭全面屏开关-执行非全面屏下处理方式 2. 非全面屏下 2.1 没有虚拟键-返回0 2.1
   * 虚拟键隐藏-返回0 2.2 虚拟键存在且未隐藏-返回虚拟键实际高度
   */
  @SuppressLint("DiscouragedApi")
  fun getNavigationBarHeight(context: Context?): Int {
    checkNotNull(context)

    if (!checkNavigationBarShow(context)) {
      return 0
    }

    val navBarHeightIdentifier =
      if ((context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE)) NAV_BAR_HEIGHT_RES_NAME
      else NAV_BAR_HEIGHT_LANDSCAPE_RES_NAME

    var result = 0
    try {
      val resourceId = context.resources.getIdentifier(navBarHeightIdentifier, "dimen", "android")
      result = context.resources.getDimensionPixelSize(resourceId)
    } catch (e: Resources.NotFoundException) {
      println("NavigationBarUtil getNavigationBarHeight: ${e.message}")
    }
    return result
  }
}