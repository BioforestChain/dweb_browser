import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test


class SegmentedButtonTest {
  @OptIn(ExperimentalMaterial3Api::class, ExperimentalTestApi::class)
  @Test
  fun testSingleChoice() = runComposeUiTest {
    setContent {
//      SegmentedButtonPreview()
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
  }
} 

