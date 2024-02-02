package org.dweb_browser.js_common.view_model

typealias EncodeValueToString = (key: String, value: dynamic, syncType: SyncType) -> String
typealias DecodeValueFromString = (key: String, value: String, syncType: SyncType) -> dynamic