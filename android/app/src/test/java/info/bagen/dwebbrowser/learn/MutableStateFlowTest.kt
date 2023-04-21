package info.bagen.dwebbrowser.learn

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class MutableStateFlowTest {
    @Test
    fun testMapAndEmit() = runBlocking {
        println("START")

        val stateFlow = MutableStateFlow(0)
        val sharedFlow = stateFlow.asSharedFlow()
//        println(sharedFlow.last())
        val job1 = launch {
            sharedFlow.collect {
                println("J1: $it")
            }
        }
        val job2 = launch {
            sharedFlow.collect {
                println("J2: $it")
            }
        }
        val job3 = launch {
            sharedFlow.collect {
                println("J3: $it")
            }
        }

        async {
            val tm = 200L;

            delay(tm)
            stateFlow.emit(1)
            job1.cancel()

            delay(tm)
            stateFlow.emit(2)
            job2.cancel()

            delay(tm)
            stateFlow.emit(3)
            job3.cancel()

            delay(tm)
            stateFlow.emit(4)


            val job4 = launch {
                sharedFlow.collect {
                    println("J4: $it")
                }
            }

            delay(tm)
            stateFlow.emit(4) // 这个不会触发，因为没有改变
            delay(tm)
            stateFlow.emit(0)
            delay(tm)
            stateFlow.emit(4)

            delay(100)
            job4.cancel()


        }

        println("END")
    }
}