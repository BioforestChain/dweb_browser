package org.dweb_browser.helper

import platform.Foundation.NSUUID

public actual fun randomUUID(): String = NSUUID.UUID().toString()