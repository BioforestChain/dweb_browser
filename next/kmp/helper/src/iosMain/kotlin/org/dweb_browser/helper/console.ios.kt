package org.dweb_browser.helper

import kotlinx.cinterop.ExperimentalForeignApi


@OptIn(ExperimentalForeignApi::class)
private val STDERR = platform.posix.fdopen(2, "w")

@OptIn(ExperimentalForeignApi::class)
public actual fun eprintln(message: String) {
  platform.posix.fprintf(STDERR, "%s\n", message)
  platform.posix.fflush(STDERR)
}
