import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.dweb_browser.js_common.state_compose.ComposeFlow
import org.dweb_browser.js_common.state_compose.state.EmitType
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TestStateComposeFlow(){
    @Test
    fun testPackagingCurrentStateOperationValueContainerString() {
        val flow = ComposeFlow.createStateComposeFlowInstance<Int, Int, String>("id")
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

    @Test
    fun testEmitByClientWithAny(){
        val flow = ComposeFlow.createStateComposeFlowInstance<Int, Int, String>("id")
        val map = mapOf<String,ComposeFlow.StateComposeFlow<*, *, *>>(
            "flow" to flow
        )
        var job: Job? = null
        val targetValue  = 1
        val deferred = CompletableDeferred<Any>()
        CoroutineScope(Dispatchers.Default).launch {
            job = map["flow"]?.collectClient{
                deferred.complete(it)
                job?.cancel()
            }
            job?.join()
            assertEquals(deferred.await(), targetValue)
            console.log("deferred.await(): ", deferred.await())
        }

        CoroutineScope(Dispatchers.Default).launch {
            map["flow"]?.emitByClient(targetValue, emitType = EmitType.REPLACE)
        }
    }

    @Test
    fun testEmitByServerWithAny() = runCommonTest{
        val flow = ComposeFlow.createStateComposeFlowInstance<Int, Int, String>("id")
        val map = mapOf<String,ComposeFlow.StateComposeFlow<*, *, *>>(
            "flow" to flow
        )
        var job: Job? = null
        val targetValue  = 1
        val deferred = CompletableDeferred<Any>()
        CoroutineScope(Dispatchers.Default).launch {
            job = map["flow"]?.collectServer{
                deferred.complete(it)
                job?.cancel()
            }
            job?.join()
        }

        CoroutineScope(Dispatchers.Default).launch {
            map["flow"]?.emitByServer(targetValue, emitType = EmitType.REPLACE)
        }
        assertEquals(deferred.await(), targetValue)
    }
}