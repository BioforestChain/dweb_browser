package org.dweb_browser.sys.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.WeakHashMap
import java.awt.Component


private val MicroModuleComponentWM = WeakHashMap<MicroModule, Component>()
var MicroModule.currentComponent
  get() = MicroModuleComponentWM[this]
  set(value) {
    MicroModuleComponentWM[this] = value
  }