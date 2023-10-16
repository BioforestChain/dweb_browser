package org.dweb_browser.helper

import kotlinx.datetime.Clock

/**
 * Epoch Milliseconds
 */
fun datetimeNow() = Clock.System.now().toEpochMilliseconds()
