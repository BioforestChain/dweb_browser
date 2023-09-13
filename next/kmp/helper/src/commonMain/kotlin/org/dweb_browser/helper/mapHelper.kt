package org.dweb_browser.helper


fun <K, V> Map<K, V>.getOrDefault(key: K, default: V) = get(key) ?: default
