package org.dweb_browser.helper.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@SuppressLint("RememberReturnType")
@Composable
fun Modifier.clickableWithNoEffect(onClick: () -> Unit) = this.clickable(indication = null,
  onClick = onClick,
  interactionSource = remember { MutableInteractionSource() }
)