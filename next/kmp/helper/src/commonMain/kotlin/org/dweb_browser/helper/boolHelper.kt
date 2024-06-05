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

inline fun <T> T.letIf(condition: Boolean, block: (T) -> T) = when {
  condition -> block(this)
  else -> this
}
