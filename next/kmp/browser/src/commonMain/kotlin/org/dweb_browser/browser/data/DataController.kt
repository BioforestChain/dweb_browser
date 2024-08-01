package org.dweb_browser.browser.data

import org.dweb_browser.browser.data.render.Render
import org.dweb_browser.dwebview.getDwebProfileStoreInstance
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.windowAdapterManager

class DataController(val storeNMM: DataNMM.DataRuntime) {
  val dWebProfileStore = getDwebProfileStoreInstance()

  fun openRender(win: WindowController) {
    windowAdapterManager.provideRender(win.id) { modifier ->
      Render(modifier = modifier, windowRenderScope = this)
    }
  }

}