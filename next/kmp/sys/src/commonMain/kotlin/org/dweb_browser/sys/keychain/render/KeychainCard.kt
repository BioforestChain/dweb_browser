package org.dweb_browser.sys.keychain.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.dweb_browser.helper.platform.theme.dimens

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
    modifier.padding(vertical = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    content()
  }
}

@Composable
internal fun CardHeader(
  modifier: Modifier = Modifier,
  background: @Composable BoxScope.() -> Unit,
  content: @Composable ColumnScope.() -> Unit,
) {
  Box(
    modifier.fillMaxWidth().height(200.dp).shadow(MaterialTheme.dimens.verySmall)
      .background(MaterialTheme.colorScheme.tertiary)
  ) {
    background()
    Column(Modifier.padding(16.dp)) {
      content()
    }
  }
}

@Composable
internal fun CardHeaderTitle(
  text: String,
  modifier: Modifier = Modifier,
  style: TextStyle? = null,
) {
  Text(
    text,
    style = MaterialTheme.typography.headlineSmall,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis
  )
}

@Composable
internal fun CardHeaderSubtitle(
  text: String,
  modifier: Modifier = Modifier,
  style: TextStyle? = null,
) {
  Text(
    text,
    style = MaterialTheme.typography.titleMedium,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis
  )
}


@Composable
internal fun CardHeaderDescription(
  text: String,
  modifier: Modifier = Modifier,
  style: TextStyle? = null,
) {
  Text(text, style = MaterialTheme.typography.bodySmall, overflow = TextOverflow.Ellipsis)
}
