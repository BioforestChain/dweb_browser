package org.dweb_browser.browser.desk

import android.content.Intent
import android.os.Bundle
import org.dweb_browser.core.module.startAppActivity

actual fun DeskNMM.startDesktopView(deskSessionId: String) {
  /// 启动对应的Activity视图，如果在后端也需要唤醒到最前面，所以需要在AndroidManifest.xml 配置 launchMode 为 singleTask
  startAppActivity(DesktopActivity::class.java) { intent ->
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
    // 不可以添加 Intent.FLAG_ACTIVITY_NEW_DOCUMENT ，否则 TaskbarActivity 就没发和 DesktopActivity 混合渲染、点击穿透
    intent.putExtras(Bundle().apply {
      putString("deskSessionId", deskSessionId)
    })
  }
}