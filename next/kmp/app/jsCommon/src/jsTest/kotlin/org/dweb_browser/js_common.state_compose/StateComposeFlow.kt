import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.dweb_browser.js_common.state_compose.ComposeFlow
import org.dweb_browser.js_common.state_compose.state.EmitType
import kotlin.test.Test


class TestStateComposeFlow(){
    @Test
    fun testPackagingCurrentStateOperationValueContainerString() {
        val flow = ComposeFlow.createStateComposeFlowInstance<Int, String>()
        val job = Job()
        CoroutineScope(Dispatchers.Default + job).launch{
            flow.collectServer{
                console.log("it: ", it)
                job.cancel()
            }
        }
        CoroutineScope(Dispatchers.Default).launch {
            flow.emitByServer(1, EmitType.REPLACE)
            job.join()

            val value = flow.packagingCurrentStateOperationValueContainerString()
            console.log("value: ", value)
        }


    }

}