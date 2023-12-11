package org.dweb_browser.helper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor

@OptIn(ExperimentalSerializationApi::class)
val CborLoose = Cbor {
  ignoreUnknownKeys = true
}