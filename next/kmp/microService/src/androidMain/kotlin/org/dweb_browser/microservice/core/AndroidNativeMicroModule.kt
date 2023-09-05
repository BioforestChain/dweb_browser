package org.dweb_browser.microservice.core

import org.dweb_browser.helper.android.BaseActivity
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
    private val activity: BaseActivity? = null
  }

  protected fun getActivity(): BaseActivity? = activity

}

