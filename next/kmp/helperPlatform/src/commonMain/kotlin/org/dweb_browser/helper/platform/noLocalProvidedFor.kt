package org.dweb_browser.helper.platform


fun noLocalProvidedFor(name: String): Nothing {
  error("[Desk]CompositionLocal $name not present")
}