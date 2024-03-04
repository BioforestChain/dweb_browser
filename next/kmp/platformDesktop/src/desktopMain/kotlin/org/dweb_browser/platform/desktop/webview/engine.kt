package org.dweb_browser.platform.desktop.webview

import com.teamdev.jxbrowser.engine.Engine
import com.teamdev.jxbrowser.engine.RenderingMode.HARDWARE_ACCELERATED
import com.teamdev.jxbrowser.engine.RenderingMode.OFF_SCREEN

object WebviewEngine {
  init {
    System.setProperty(
      "jxbrowser.license.key",
      //
      "1BNDIEOFAZ1Z8R8VNNG4W07HLC9173JJW3RT0P2G9Y28L9YFFIWDBRFNFLFDQBKXAHO9ZE" // Only For JxBrowser 7.26
    );
  }

  val hardwareAccelerated by lazy { Engine.newInstance(HARDWARE_ACCELERATED) }
  val offScreen by lazy { Engine.newInstance(OFF_SCREEN) }
}
