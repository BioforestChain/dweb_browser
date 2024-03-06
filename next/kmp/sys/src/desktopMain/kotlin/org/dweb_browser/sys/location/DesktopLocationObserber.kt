package org.dweb_browser.sys.location

import kotlinx.coroutines.flow.MutableSharedFlow

class DesktopLocationObserver() : LocationObserver() {
  private val sharedFlow = MutableSharedFlow<GeolocationPosition>()
  override val flow get() = sharedFlow
  override fun start(precise: Boolean, minTimeMs: Long, minDistance: Double) {
    TODO("Not yet implemented")
  }

  override fun stop() {
    TODO("Not yet implemented")
  }

}