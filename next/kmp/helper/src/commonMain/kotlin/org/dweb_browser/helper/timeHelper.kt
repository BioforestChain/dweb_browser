package org.dweb_browser.helper

import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

/**
 * Epoch Milliseconds
 */
@OptIn(ExperimentalTime::class)
public fun datetimeNow(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * 获取从1970到现在的天数
 */
@OptIn(ExperimentalTime::class)
public fun datetimeNowToEpochDay(): Long = Clock.System.now().toEpochDay()

/**
 * 转为天数
 */
@OptIn(ExperimentalTime::class)
public fun Long.toEpochDay(): Long {
  return Instant.fromEpochMilliseconds(this).toEpochDay()
}

/**
 * 比对天数差，返回0表示同一天，正数表示大于当前时间的天数，负数表示小于当前时间的天数
 */
@OptIn(ExperimentalTime::class)
public fun Long.compareNowDayByMilliseconds(): Long {
  val lastInstant = Instant.fromEpochMilliseconds(this)
  val nowInstant = Clock.System.now()
  return lastInstant.minus(nowInstant, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
}

/**
 * 转化为时间格式
 */
@OptIn(ExperimentalTime::class)
public fun Long.formatTimestampByMilliseconds(): String {
  val instant = Instant.fromEpochMilliseconds(this)
  val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
  return "${localDateTime.date} ${localDateTime.time}"
}

/**
 * 转化为日期格式
 */
@OptIn(ExperimentalTime::class)
public fun Long.formatDatestampByMilliseconds(): String {
  val instant = Instant.fromEpochMilliseconds(this)
  val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
  return localDateTime.date.toString()
}

@OptIn(ExperimentalTime::class)
public fun Long.formatDatestampByEpochDay(): String {
  val instant = Instant.fromEpochDays(this)
  val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
  return localDateTime.date.toString()
}

/**
 * The number of days in a 400 year cycle.
 */
private const val DAYS_PER_CYCLE = 146097

/**
 * The number of days from year zero to year 1970. There are five 400 year cycles from year zero to 2000.
 * There are 7 leap years from 1970 to 2000.
 */
private const val DAYS_0000_TO_1970 = (DAYS_PER_CYCLE * 5L) - (30L * 365L + 7L)

/**
 * 计算从1970年到现在的天数
 */
@OptIn(ExperimentalTime::class)
private fun Instant.toEpochDay(): Long {
  val localDateTime = this.toLocalDateTime(TimeZone.currentSystemDefault())
  val year = localDateTime.year
  val month = localDateTime.monthNumber
  val day = localDateTime.dayOfMonth
  var total = 0L
  total += 365 * year
  if (year >= 0) {
    total += (year + 3) / 4 - (year + 99) / 100 + (year + 399) / 400
  } else {
    total -= year / -4 - year / -100 + year / -400
  }
  total += ((367 * month - 362) / 12)
  total += day - 1
  if (month > 2) {
    total--
    if (!isLeapYear(year)) {
      total--
    }
  }
  return total - DAYS_0000_TO_1970
}

private fun isLeapYear(year: Int): Boolean {
  return (year and 3) == 0 && (year % 100 != 0 || year % 400 == 0)
}

/**
 * 将天数的转为Instant对象
 */
@OptIn(ExperimentalTime::class)
private fun Instant.Companion.fromEpochDays(days: Long): Instant {
  val milliseconds = days * 24 * 60 * 60 * 1000
  return fromEpochMilliseconds(milliseconds)
}