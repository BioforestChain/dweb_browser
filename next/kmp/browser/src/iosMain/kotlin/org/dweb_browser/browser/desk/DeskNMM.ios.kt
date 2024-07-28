package org.dweb_browser.browser.desk

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.module.startUIViewController
import org.dweb_browser.helper.platform.PureViewController

data class StartTabletopViewParams(val deskSessionId: String)

private var preParams = StartTabletopViewParams("")
val lock = Mutex();
actual suspend fun DeskNMM.DeskRuntime.startTabletopView(deskSessionId: String) = lock.withLock {
//  withMainContext {
//     println("startUIViewController:${TabletopUIViewController::class}")
//    val rvc = getUIApplication().keyWindow?.rootViewController ?: return@withMainContext
  val newParams = StartTabletopViewParams(deskSessionId)
  if (newParams == preParams) return@withLock
  preParams = newParams
//    val pvc = PureViewController(rvc)
//    TabletopUIViewController(pvc)
//    pvc.emitCreateSignal(PureViewCreateParams(mapOf("deskSessionId" to deskSessionId)))
//    val vc = pvc.getContent()
//    rvc.addChildViewController(vc)
//    rvc.view.addSubview(vc.view)
////  rvc.view.addSubview(pvc.getContent().view)
//  }
  /// 启动对应的Activity视图，如果在后端也需要唤醒到最前面，所以需要在AndroidManifest.xml 配置 launchMode 为 singleTask
  startUIViewController(PureViewController(mapOf("deskSessionId" to deskSessionId)).also {
    TabletopUIViewController(it)
  })

}
