package org.dweb_browser.helper

inline fun Boolean.trueAlso(block: () -> Unit): Boolean {
  if (this) {
    block()
  }
  return this
}

inline fun Boolean.falseAlso(block: () -> Unit): Boolean {
  if (!this) {
    block()
  }
  return this
}
