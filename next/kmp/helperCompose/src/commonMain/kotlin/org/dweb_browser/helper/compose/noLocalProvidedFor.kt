package org.dweb_browser.helper.compose


fun noLocalProvidedFor(name: String): Nothing {
  error("[Desk]CompositionLocal $name not present")
}