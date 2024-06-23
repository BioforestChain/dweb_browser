package org.dweb_browser.helper.compose

import androidx.navigation.NavHostController

fun NavHostController.navigateOrUp(route: String) {
  val stackList = currentBackStack.value
  when (val upIndex = stackList.indexOfLast { it.destination.route == route }) {
    -1 -> navigate(route)
    else -> {
      var upCount = stackList.size - 1 - upIndex
      while (upCount-- > 0) {
        navigateUp()
      }
    }
  }
}