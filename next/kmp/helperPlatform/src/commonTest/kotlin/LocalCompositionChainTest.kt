import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.compositionChainOf
import kotlin.test.Test

class LocalCompositionChainTest {
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun testProvider() = runComposeUiTest {
    val LocalTest = compositionChainOf<String>("test")
    val LocalOther = compositionChainOf<Int>("other")
    val testState = mutableStateOf("1")
    setContent {
      LocalCompositionChain.current.Provider(
        LocalTest provides testState.value
      ) {
        Text(
          text = LocalTest.current,
          modifier = Modifier.testTag("test1")
        )
        println("LocalCompositionChain.current before = ${LocalCompositionChain.current.providerMap}")
        LocalCompositionChain.current.Provider(
          LocalOther provides 0
        ) {
          println("LocalCompositionChain.current after = ${LocalCompositionChain.current.providerMap}")
          Text(
            text = LocalTest.current,
            modifier = Modifier.testTag("test2")
          )
        }
      }
    }
    onNodeWithTag("test1").assertTextEquals(testState.value)
    onNodeWithTag("test2").assertTextEquals(testState.value)
    testState.value = "2"
    onNodeWithTag("test1").assertTextEquals(testState.value)
    onNodeWithTag("test2").assertTextEquals(testState.value)
  }
}