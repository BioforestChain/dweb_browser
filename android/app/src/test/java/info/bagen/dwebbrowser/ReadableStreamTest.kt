package info.bagen.dwebbrowser

import info.bagen.dwebbrowser.microService.core.BootstrapContext
import info.bagen.dwebbrowser.microService.core.NativeMicroModule
import info.bagen.dwebbrowser.microService.helper.Signal
import info.bagen.dwebbrowser.microService.helper.text
import info.bagen.dwebbrowser.microService.ipc.*
import info.bagen.dwebbrowser.microService.helper.toByteArray
import kotlinx.coroutines.*
import org.http4k.core.Method
import org.http4k.core.Request
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

class ReadableStreamTest : AsyncBase() {
    @Test
    fun base() = runBlocking {
        class Event(val target: ReadableStream.ReadableStreamController, val data: String)

        val i = 0
        val mm = Signal<Event>()
        mm.listen {
//            async {
            if (it.data == "pull") {
                it.target.enqueue(i.toByteArray())
            }
//            }
        }

        val m1 = object : NativeMicroModule("m1") {
            override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
                TODO("Not yet implemented")
            }

            override suspend fun _shutdown() {
                TODO("Not yet implemented")
            }
        }


        val m2 = object : NativeMicroModule("m2") {
            override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
                TODO("Not yet implemented")
            }

            override suspend fun _shutdown() {
                TODO("Not yet implemented")
            }
        }

        val req_ipc = ReadableStreamIpc(m1, IPC_ROLE.CLIENT)
        val res_ipc = ReadableStreamIpc(m2, IPC_ROLE.SERVER)
        res_ipc.bindIncomeStream(req_ipc.stream, "from-req")


        res_ipc.onRequest { (request, ipc) ->
            println("req get request $request")
            delay(200)
            println("echo after 1s $request")
            ipc.postMessage(
                IpcResponse.fromText(
                    request.req_id,
                    200,
                    IpcHeaders(),
                    "ECHO:" + request.body.base64(),
                    ipc
                )
            )
        }


        delay(100)
        req_ipc.bindIncomeStream(res_ipc.stream, "to-res")
        for (i in 1..10) {
            println("开始发送 $i")
            val req = Request(Method.GET, "").body("hi-$i")
            val res = req_ipc.request(req)
            assertEquals(res.text(), "ECHO:" + req.bodyString())
        }
        req_ipc.close()

        req_ipc.stream.afterClosed()
    }


    @Test
    fun doubleAvailable() = runBlocking {
        println("start")
        val stream = ReadableStream(onStart = { controller ->
            launch {
                delay(1000)
                controller.enqueue(byteArrayOf(1, 2, 3))
                println("enqueued")
            }
        })

        var result = AtomicInteger(0)
        for (i in 1..10) {
            GlobalScope.launch {
                delay(100)
                val len = stream.available()
                println("stream.available(): $len")
                result.addAndGet(len)
            }
        }

        async {
            delay(2000)
        }.join()
        assertEquals(result.get(), 10 * 3)
    }
}