package info.bagen.dwebbrowser.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources

object FontDisplayUtil {

  /**
   * 保持字体大小不随系统设置变化（用在界面加载之前）
   * 要重写Activity的attachBaseContext()
   */
  fun attachBaseContext(context: Context, fontScale: Float): Context? {
    val config: Configuration = context.resources.configuration
    //正确写法
    config.fontScale = fontScale
    return context.createConfigurationContext(config)
  }

  /**
   * 保持字体大小不随系统设置变化（用在界面加载之前）
   * 要重写Activity的getResources()
   */
  fun getResources(context: Context, resources: Resources, fontScale: Float): Resources {
    val config: Configuration = resources.configuration
    return if (config.fontScale != fontScale) {
      config.fontScale = fontScale
      context.createConfigurationContext(config).getResources()
    } else {
      resources
    }
  }

  /**
   * 保存字体大小，后通知界面重建，它会触发attachBaseContext，来改变字号
   */
  fun recreate(activity: Activity) {
    activity.recreate()
  }
}