package org.dweb_browser.helper.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.withMainContext
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController


private var ios_rootUIViewControllerCreateHook_accId = 0;
private var ios_rootUIViewControllerCreateHook: (Int) -> Unit = {}


fun nativeRootUIViewController_setCreateHook(hook: (Int) -> Unit) {
  ios_rootUIViewControllerCreateHook = hook
}

private var ios_rootUIViewControllerUpdateHook: (UIViewController) -> Unit = {}

fun nativeRootUIViewController_setUpdateHook(hook: (UIViewController) -> Unit) {
  ios_rootUIViewControllerUpdateHook = hook
}


val nativeRootUIViewController_onInitSignal = Signal<Pair<Int, UIViewController>>();
val nativeRootUIViewController_onCreateSignal = Signal<Int>();
val nativeRootUIViewController_onResumeSignal = Signal<Int>();
val nativeRootUIViewController_onPauseSignal = Signal<Int>();
val nativeRootUIViewController_onDestroySignal = Signal<Int>();
private val createLock = Mutex()

@OptIn(ExperimentalForeignApi::class)
suspend fun nativeRootUIViewController_start(pureViewController: PureViewController) =
  createLock.withLock {
    withMainContext {
      when (val parentVc = pureViewController.getUIViewControllerSync()) {
        null -> {
          /// 定制一个行的vcId
          val vcId = ios_rootUIViewControllerCreateHook_accId++
          pureViewController.setVcId(vcId)
          val vcDeferred = CompletableDeferred<UIViewController>()
          /// 设置init监听，等待vc构建完成
          nativeRootUIViewController_onInitSignal.listen { (id, vc) ->
            if (id == vcId) {
              vcDeferred.complete(vc)
              offListener()
            }
          }
          // 唤醒swift进行重新绘制一个新的UIViewController
          ios_rootUIViewControllerCreateHook(vcId)
          val parentVc = vcDeferred.await()
          pureViewController.setUIViewController(parentVc)
          val viewVc = pureViewController.getContent()
          /// 对这个vc进行视图的初始化操作
          parentVc.addChildViewController(viewVc)
          parentVc.view.addSubview(viewVc.view)
          viewVc.view.setFrame(parentVc.view.frame)
          viewVc.didMoveToParentViewController(parentVc)
          parentVc
        }

        else -> {
          ios_rootUIViewControllerUpdateHook(parentVc)
        }
      }
    }
  }
