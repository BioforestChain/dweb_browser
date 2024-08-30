package org.dweb_browser.helper

import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSString
import platform.Foundation.create

// Turn casts into dot calls for better readability
@OptIn(BetaInteropApi::class)
public fun String.toNSString(): NSString = NSString.create(string = this)

@Suppress("CAST_NEVER_SUCCEEDS")
public fun NSString.toKString(): String = this as String