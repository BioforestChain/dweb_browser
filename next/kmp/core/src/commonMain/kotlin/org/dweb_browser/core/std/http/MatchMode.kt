package org.dweb_browser.core.std.http

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.StringEnumSerializer

object MatchModeSerializer :
  StringEnumSerializer<MatchMode>("MatchMode", MatchMode.ALL_VALUES, { mode })

@Serializable(MatchModeSerializer::class)
enum class MatchMode(val mode: String) {
  FULL("full"), PREFIX("prefix"), ;

  companion object {
    val ALL_VALUES = entries.associateBy { it.mode }
  }
}