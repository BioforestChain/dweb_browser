package org.dweb_browser.browser.bookmark

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun RowItemBook(
  data: Bookmark,
  modifier: Modifier = Modifier,
  trailingContent: (@Composable () -> Unit)? = null
) {
  ListItem(
    modifier = modifier,
    leadingContent = {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(shape = CircleShape)
          .background(
            MaterialTheme.colorScheme.secondaryContainer
          )
          .padding(6.dp), contentAlignment = Alignment.Center
      ) {
        if (data.icon == null) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .clip(shape = CircleShape)
              .background(Color.White.copy(alpha = 0.3f)), contentAlignment = Alignment.Center
          ) {
            Text(
              text = data.title[0].toString(),
              modifier = Modifier,
              style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSecondaryContainer)
            )
          }
        } else {
          Image(
            bitmap = data.icon,
            contentDescription = "Favorite",
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    },
    headlineContent = {
      Text(text = data.title)
    },
    supportingContent = {
      Text(text = data.url)
    },
    trailingContent = trailingContent ?: {
      Icon(
        Icons.Filled.OpenInNew,
        contentDescription = "Open",
        modifier = Modifier
          .size(24.dp)
          .padding(4.dp)
      )
    },
  )
}

@Preview
@Composable
fun PreviewRowItemBook1() {
  RowItemBook(
    data = Bookmark(
      title = "Example Title",
      url = "https://google.com",
    )
  )
}

@Preview
@Composable
fun PreviewRowItemBook2() {
  RowItemBook(
    data = Bookmark(
      title = "Example Title",
      url = "https://google.com",
    )
  )
}
