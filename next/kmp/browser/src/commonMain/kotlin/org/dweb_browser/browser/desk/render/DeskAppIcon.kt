package org.dweb_browser.browser.desk.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import org.dweb_browser.sys.window.render.AppIcon
import org.dweb_browser.sys.window.render.AppIconContainer
import org.dweb_browser.sys.window.render.AppLogo

@Composable
fun AppLogo.toDeskAppLogo() = remember(this) { copyToDeskAppLogo() }
fun AppLogo.copyToDeskAppLogo() = if (color == null) withColor(color = Color.Black) else this

@Composable
fun AppLogo.toDeskAppIcon(
  containerBase: AppIconContainer? = null,
  containerColor: Color? = null,
  containerAlpha: Float? = null,
) =
  toDeskAppLogo().toIcon(
    when (containerBase) {
      null -> AppIconContainer(
        color = containerColor ?: Color.White,
        alpha = containerAlpha ?: deskIconAlpha
      )

      else -> containerBase.withColorAndAlpha(
        color = containerColor ?: Color.White,
        alpha = containerAlpha ?: deskIconAlpha
      )
    }
  )

@Composable
fun AppIcon.toDeskAppIcon() =
  logo.toDeskAppLogo().toIcon(container.withColorAndAlpha(color = Color.White, alpha = deskIconAlpha))

internal val deskIconAlpha = when {
  canSupportModifierBlur() -> 0.9f
  else -> 1f
}
