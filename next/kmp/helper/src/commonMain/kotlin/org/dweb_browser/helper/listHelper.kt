package org.dweb_browser.helper

fun <E> List<E>.one(function: (it: E) -> Unit): E? {
  for (item in this) {
    function(item)
    return item
  }
  return null
}

fun <E> List<E>.all(function: (it: E) -> Unit): List<E>? {
  if (isEmpty()) {
    return null
  }
  for (item in this) {
    function(item)
  }
  return this
}

fun <E, R> List<E>.mapFindNoNull(function: (it: E) -> R): R? {
  for (item in this) {
    val res = function(item)
    if (res != null) {
      return res
    }
  }
  return null
}

fun <E> List<E>.someOrNull(function: (it: E) -> Boolean): Boolean? {
  for (item in this) {
    val res = function(item)
    if (res) {
      return true
    }
  }
  return null
}

fun <E> List<E>.some(function: (it: E) -> Boolean) = this.someOrNull(function) ?: false