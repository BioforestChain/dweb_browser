package org.dweb_browser.platform.desktop.webview

import com.teamdev.jxbrowser.engine.Engine
import com.teamdev.jxbrowser.engine.EngineOptions
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

  private val engine: Engine? = null

  fun hardwareAccelerated(optionsBuilder: (EngineOptions.Builder.() -> Unit)? = null): Engine {
    if(engine == null) {
      Engine.newInstance(
        EngineOptions.newBuilder(HARDWARE_ACCELERATED).run {
          optionsBuilder?.invoke(this)
          build()
        })
    }

    return engine!!
  }


  fun offScreen(optionsBuilder: (EngineOptions.Builder.() -> Unit)? = null): Engine =
    Engine.newInstance(
      EngineOptions.newBuilder(OFF_SCREEN).run {
        optionsBuilder?.invoke(this)
        build()
      })

  val offScreen by lazy { offScreen() }
}
