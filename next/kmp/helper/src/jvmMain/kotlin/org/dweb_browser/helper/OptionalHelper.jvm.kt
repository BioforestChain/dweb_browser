package org.dweb_browser.helper

import java.util.Optional

public fun <T> Optional<T>.getOrElse(or: () -> T): T = if (isPresent) get() else or()
public fun <T> Optional<T>.getOrNull(): T? = if (isPresent) get() else null

public fun <T, R : Any?> Optional<T>.runIfOrElse(then: (T) -> R, or: () -> R): R =
  if (isPresent) then(get()) else or()

public fun <T, R : Any?> Optional<T>.runIf(then: (T) -> R): R? =
  if (isPresent) then(get()) else null
