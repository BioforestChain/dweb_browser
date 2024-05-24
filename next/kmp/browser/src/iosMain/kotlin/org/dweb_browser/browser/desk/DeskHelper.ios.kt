package org.dweb_browser.browser.desk

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browser.desk.upgrade.NewVersionItem
import org.dweb_browser.core.module.startUIViewController
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.WARNING

data class StartDesktopViewParams(val deskSessionId: String)

private var preParams = StartDesktopViewParams("")
val lock = Mutex();
actual suspend fun DeskNMM.DeskRuntime.startDesktopView(deskSessionId: String) = lock.withLock {
//  withMainContext {
//     println("startUIViewController:${DesktopUIViewController::class}")
//    val rvc = getUIApplication().keyWindow?.rootViewController ?: return@withMainContext
  val newParams = StartDesktopViewParams(deskSessionId)
  if (newParams == preParams) return@withLock
  preParams = newParams
//    val pvc = PureViewController(rvc)
//    DesktopUIViewController(pvc)
//    pvc.emitCreateSignal(PureViewCreateParams(mapOf("deskSessionId" to deskSessionId)))
//    val vc = pvc.getContent()
//    rvc.addChildViewController(vc)
//    rvc.view.addSubview(vc.view)
////  rvc.view.addSubview(pvc.getContent().view)
//  }
  /// 启动对应的Activity视图，如果在后端也需要唤醒到最前面，所以需要在AndroidManifest.xml 配置 launchMode 为 singleTask
  startUIViewController(PureViewController(mapOf("deskSessionId" to deskSessionId)).also {
    DesktopUIViewController(it)
  })

}

actual suspend fun loadApplicationNewVersion(): NewVersionItem? {
  WARNING("Not yet implement loadNewVersion")
  return null
}

actual fun desktopGridLayout(): GridCells = GridCells.Fixed(4)

actual fun desktopTap(): Dp = 0.dp