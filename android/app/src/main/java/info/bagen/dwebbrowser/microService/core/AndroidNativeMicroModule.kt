package info.bagen.dwebbrowser.microService.core

import info.bagen.dwebbrowser.base.BaseThemeActivity
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.types.MMID
import org.dweb_browser.microservice.help.types.MicroModuleManifest

abstract class AndroidNativeMicroModule(manifest: MicroModuleManifest) :
  NativeMicroModule(manifest) {
  constructor(mmid: MMID, name: String) : this(
    MicroModuleManifest().apply {
      this.mmid = mmid
      this.name = name
    }
  )

  companion object {
    //  管理所有的activity
    private val activity: BaseThemeActivity? = null
  }

  protected fun getActivity(): BaseThemeActivity? = activity

}

