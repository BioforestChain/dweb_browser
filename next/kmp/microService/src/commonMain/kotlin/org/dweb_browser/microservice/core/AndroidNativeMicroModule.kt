package org.dweb_browser.microservice.core

import android.content.Context
import org.dweb_browser.helper.android.BaseActivity
import org.dweb_browser.microservice.help.types.MMID
import org.dweb_browser.microservice.help.types.MicroModuleManifest

abstract class AndroidNativeMicroModule(manifest: MicroModuleManifest) :
  org.dweb_browser.microservice.core.NativeMicroModule(manifest) {
  constructor(mmid: MMID, name: String) : this(
    MicroModuleManifest().apply {
      this.mmid = mmid
      this.name = name
    }
  )

  companion object {
    //  管理所有的activity
    private val activity: BaseActivity? = null
    lateinit var appContext: Context
  }

  protected fun getActivity(): BaseActivity? = activity
  protected fun getAppContext() = appContext
}

