package org.dweb_browser.helper

import web.crypto.crypto


actual fun randomUUID(): UUID = crypto.randomUUID()
