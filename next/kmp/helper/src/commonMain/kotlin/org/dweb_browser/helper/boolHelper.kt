package org.dweb_browser.helper

inline fun Boolean.ifTrue(block: () -> Unit): Boolean {
  if (this) {
    block()
  }
  return this
}

inline fun Boolean.ifFalse(block: () -> Unit): Boolean {
  if (!this) {
    block()
  }
  return this
}