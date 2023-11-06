package org.dweb_browser.helper

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Epoch Milliseconds
 */
fun datetimeNow() = Clock.System.now().toEpochMilliseconds()

fun Long.formatTimestamp(): String {
  val instant = Instant.fromEpochMilliseconds(this)
  val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
  return "${localDateTime.date} ${localDateTime.time}"
}

fun Long.formatDatestamp(): String {
  val instant = Instant.fromEpochMilliseconds(this)
  val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
  return localDateTime.date.toString()
}