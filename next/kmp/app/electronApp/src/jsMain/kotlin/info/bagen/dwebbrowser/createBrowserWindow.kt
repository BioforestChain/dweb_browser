package info.bagen.dwebbrowser

import electron.BrowserWindow
import electron.BrowserWindowConstructorOptions

fun createBrowserWindow(optionsBuilder: BrowserWindowConstructorOptions.() -> Unit): BrowserWindow {
  @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
  return BrowserWindow((object {} as BrowserWindowConstructorOptions).also(optionsBuilder))
}