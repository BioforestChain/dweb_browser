package org.dweb_browser.helper.android


fun noLocalProvidedFor(name: String): Nothing {
  error("[Desk]CompositionLocal $name not present")
}