package org.dweb_browser.helper

import java.util.Optional

fun <T> Optional<T>.getOrElse(or: () -> T) = if (isPresent) get() else or()
fun <T> Optional<T>.getOrNull() = if (isPresent) get() else null

fun <T, R : Any?> Optional<T>.runIfOrElse(then: (T) -> R, or: () -> R) =
  if (isPresent) then(get()) else or()

fun <T, R : Any?> Optional<T>.runIf(then: (T) -> R) =
  if (isPresent) then(get()) else null
