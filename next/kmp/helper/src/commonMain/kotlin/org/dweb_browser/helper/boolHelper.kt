package org.dweb_browser.helper

public inline fun Boolean.trueAlso(block: () -> Unit): Boolean {
  if (this) {
    block()
  }
  return this
}

public inline fun Boolean.falseAlso(block: () -> Unit): Boolean {
  if (!this) {
    block()
  }
  return this
}

public inline fun <T> T.letIf(condition: Boolean, block: (T) -> T): T = when {
  condition -> block(this)
  else -> this
}
