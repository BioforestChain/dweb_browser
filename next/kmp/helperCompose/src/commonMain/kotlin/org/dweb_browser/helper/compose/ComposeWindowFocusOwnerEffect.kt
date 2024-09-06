package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable

@Composable
expect fun ComposeWindowFocusOwnerEffect(show: Boolean, onDismiss: () -> Unit)
