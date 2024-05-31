package org.dweb_browser.helper.platform


import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.WeakHashMap

private val MM_PureViewController_WM = WeakHashMap<MicroModule.Runtime, IPureViewController>()
fun MicroModule.Runtime.getPureViewControllerOrNull() = MM_PureViewController_WM.get(this)
fun MicroModule.Runtime.getRootPureViewControllerOrNull() = rootPureViewController
private var rootPureViewController: IPureViewController? = null
fun MicroModule.Runtime.bindPureViewController(
  pureViewController: IPureViewController,
  root: Boolean = false,
) {
  MM_PureViewController_WM.put(this, pureViewController)
  if (root) {
    rootPureViewController = pureViewController
  }
}

fun MicroModule.Runtime.unbindPureViewController(pureViewController: IPureViewController? = null): Boolean {
  if (pureViewController != null) {
    if (MM_PureViewController_WM.get(this) != pureViewController) {
      return false
    }
  }
  MM_PureViewController_WM.remove(this)
  return true
}