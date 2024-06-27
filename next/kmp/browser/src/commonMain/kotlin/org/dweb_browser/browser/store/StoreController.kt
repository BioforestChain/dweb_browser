package org.dweb_browser.browser.store

import org.dweb_browser.browser.store.render.Render
import org.dweb_browser.dwebview.getDwebProfileStoreInstance
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.windowAdapterManager

class StoreController(val storeNMM: StoreNMM.StoreRuntime) {
  val dWebProfileStore = getDwebProfileStoreInstance()

  fun openRender(win: WindowController) {
    windowAdapterManager.provideRender(win.id) { modifier ->
      Render(modifier = modifier, windowRenderScope = this)
    }
  }

}