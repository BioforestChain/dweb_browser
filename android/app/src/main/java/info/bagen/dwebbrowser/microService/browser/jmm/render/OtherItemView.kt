package info.bagen.dwebbrowser.microService.browser.jmm.render

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * @param largeContent 该字段如果有数据，表示允许展开，查看详细信息
 */
@Composable
internal fun OtherItemView(type: String, content: String, largeContent: String? = null) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = type, color = MaterialTheme.colorScheme.outline)

      Text(
        text = content,
        modifier = Modifier.weight(1f),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    CustomerDivider()
  }
}