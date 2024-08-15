package org.dweb_browser.helper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor

@OptIn(ExperimentalSerializationApi::class)
public val CborLoose: Cbor = Cbor {
  ignoreUnknownKeys = true
}