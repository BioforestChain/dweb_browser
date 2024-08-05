package org.dweb_browser.helper

fun <E> List<E>.one(function: (it: E) -> Unit): E? {
  for (item in this) {
    function(item)
    return item
  }
  return null
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

fun <E> List<E>.safeSubList(fromIndex: Int, toIndex: Int): List<E> {
  if(isEmpty()) return emptyList()
  val validFromIndex = fromIndex.coerceIn(0, this.size - 1)
  val validToIndex = toIndex.coerceIn(0, this.size)
  return if (validFromIndex < validToIndex) {
    this.subList(validFromIndex, validToIndex)
  } else {
    emptyList()
  }
}
