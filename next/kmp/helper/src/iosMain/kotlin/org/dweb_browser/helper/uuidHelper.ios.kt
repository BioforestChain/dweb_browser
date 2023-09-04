package org.dweb_browser.helper

import platform.Foundation.NSUUID

actual fun randomUUID(): String = NSUUID.UUID().toString()