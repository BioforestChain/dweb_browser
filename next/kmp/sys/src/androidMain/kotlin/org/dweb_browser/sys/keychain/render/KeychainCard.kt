package org.dweb_browser.sys.keychain.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
internal fun CardTitle(text: String, modifier: Modifier = Modifier, style: TextStyle? = null) {
  Text(
    text,
    style = MaterialTheme.typography.titleMedium.merge(style),
    modifier = modifier.padding(bottom = 8.dp)
  )
}

@Composable
internal fun CardSubTitle(text: String, modifier: Modifier = Modifier, style: TextStyle? = null) {
  Text(
    text,
    style = MaterialTheme.typography.labelMedium.merge(style),
    modifier = modifier.padding(bottom = 8.dp)
  )
}

@Composable
internal fun CardSubTitle(
  text: AnnotatedString,
  modifier: Modifier = Modifier,
  style: TextStyle? = null,
) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelMedium.merge(style),
    modifier = modifier.padding(bottom = 8.dp)
  )
}

@Composable
internal fun CardDescription(
  text: String,
  modifier: Modifier = Modifier,
  style: TextStyle? = null,
) {
  Text(
    text,
    style = MaterialTheme.typography.bodyMedium.merge(style),
    modifier = modifier.padding(bottom = 8.dp)
  )
}

@Composable
internal fun CardActions(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
  Row(
    modifier.padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    content()
  }
}