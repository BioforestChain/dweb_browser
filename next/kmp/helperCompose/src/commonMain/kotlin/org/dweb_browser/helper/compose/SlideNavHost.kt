package org.dweb_browser.helper.compose

import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

@Composable
fun SlideNavHost(
  navController: NavHostController,
  startDestination: String,
  modifier: Modifier = Modifier,
  contentAlignment: Alignment = Alignment.Center,
  route: String? = null,
  builder: NavGraphBuilder.() -> Unit,
) {
  NavHost(
    navController = navController,
    startDestination = startDestination,
    modifier = modifier,
    contentAlignment = contentAlignment,
    route = route,
    // 新页面进场
    enterTransition = { SlideNavAnimations.enterTransition },
    // 新页面退场
    popExitTransition = { SlideNavAnimations.popExitTransition },

    // 旧页面退场
    exitTransition = { SlideNavAnimations.exitTransition },
    // 旧页面回场
    popEnterTransition = { SlideNavAnimations.popEnterTransition },

    builder = builder
  )
}

object SlideNavAnimations {
  // 新页面进场
  val enterTransition = slideIn(iosTween(true)) { IntOffset(it.width, 0) }

  // 新页面退场
  val popExitTransition = slideOut(iosTween(false)) { IntOffset(it.width, 0) }

  // 旧页面退场
  val exitTransition = slideOut(iosTween(false)) { IntOffset(-it.width, 0) }

  // 旧页面回场
  val popEnterTransition = slideIn(iosTween(true)) { IntOffset(-it.width, 0) }
}