package org.dweb_browser.browser.desk

import org.dweb_browser.core.module.getUIApplication
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.withMainContext

actual suspend fun DeskNMM.startDesktopView(deskSessionId: String) = withMainContext {
  /// 启动对应的Activity视图，如果在后端也需要唤醒到最前面，所以需要在AndroidManifest.xml 配置 launchMode 为 singleTask
//  startUIViewController(DesktopUIViewController::class)
  println("startUIViewController:${DesktopUIViewController::class}")
  val rvc = getUIApplication().keyWindow?.rootViewController ?: return@withMainContext
  val pvc = PureViewController(rvc)
  DesktopUIViewController(pvc)
  rvc.showViewController(pvc.getContent(), null)
//  rvc.view.addSubview(pvc.getContent().view)
}