package org.dweb_browser.helper

import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSString
import platform.Foundation.create

// Turn casts into dot calls for better readability
@OptIn(BetaInteropApi::class)
inline fun String.toNSString() = NSString.create(string = this)

@Suppress("CAST_NEVER_SUCCEEDS")
inline fun NSString.toKString() = this as String