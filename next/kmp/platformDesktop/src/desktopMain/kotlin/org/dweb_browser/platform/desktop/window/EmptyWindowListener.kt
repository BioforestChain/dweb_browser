package org.dweb_browser.platform.desktop.window

import java.awt.event.WindowEvent
import java.awt.event.WindowListener

open class EmptyWindowListener : WindowListener {
  override fun windowOpened(event: WindowEvent) {
  }

  override fun windowClosing(event: WindowEvent) {
  }

  override fun windowClosed(event: WindowEvent) {
  }

  override fun windowIconified(event: WindowEvent) {
  }

  override fun windowDeiconified(event: WindowEvent) {
  }

  override fun windowActivated(event: WindowEvent) {
  }

  override fun windowDeactivated(event: WindowEvent) {
  }
}