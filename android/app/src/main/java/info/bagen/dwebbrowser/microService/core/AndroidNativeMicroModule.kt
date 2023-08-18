package info.bagen.dwebbrowser.microService.core

import info.bagen.dwebbrowser.base.BaseThemeActivity
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.MMID

abstract class AndroidNativeMicroModule(override val mmid: MMID, override val name: String) :
  NativeMicroModule(mmid, name) {

  companion object {
    //  管理所有的activity
    private val activity: BaseThemeActivity? = null
  }

  protected fun getActivity(): BaseThemeActivity? = activity

}

