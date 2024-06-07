import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun SegmentedButtonPreview() {
  var selectedIndex by remember { mutableStateOf(0) }
  val options = listOf("Day", "Month", "Week")
  SingleChoiceSegmentedButtonRow {
    options.forEachIndexed { index, label ->
      SegmentedButton(
        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
        onClick = { selectedIndex = index },
        selected = index == selectedIndex
      ) {
        Text(label)
      }
    }
  }
}