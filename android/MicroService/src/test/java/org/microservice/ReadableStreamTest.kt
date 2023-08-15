package org.microservice

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.iterator
import org.dweb_browser.helper.toByteArray
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.text
import org.dweb_browser.microservice.ipc.ReadableStreamIpc
import org.dweb_browser.microservice.ipc.helper.IPC_ROLE
import org.dweb_browser.microservice.ipc.helper.IpcHeaders
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.dweb_browser.microservice.ipc.helper.ReadableStream
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

        val m1 = object : NativeMicroModule("m1","m1") {
            override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
                TODO("Not yet implemented")
            }

            override suspend fun _shutdown() {
                TODO("Not yet implemented")
            }
        }


        val m2 = object : NativeMicroModule("m2","m2") {
            override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
                TODO("Not yet implemented")
            }

            override suspend fun _shutdown() {
                TODO("Not yet implemented")
            }
        }

        val req_ipc = ReadableStreamIpc(m1, IPC_ROLE.CLIENT)
        val res_ipc = ReadableStreamIpc(m2, IPC_ROLE.SERVER)
        res_ipc.bindIncomeStream(req_ipc.stream)


        res_ipc.onRequest { (request, ipc) ->
            println("req get request $request")
            delay(200)
            println("echo after 1s $request")
            ipc.postMessage(
                IpcResponse.fromText(
                    request.req_id,
                    200,
                    IpcHeaders(),
                    "ECHO:" + request.body.text(),
                    ipc
                )
            )
        }


        delay(100)
        req_ipc.bindIncomeStream(res_ipc.stream)
        for (i in 1..10) {
            println("开始发送 $i")
            val req = Request(Method.POST, "").body("hi-$i")
            val res = req_ipc.request(req)
            assertEquals(res.text(), "ECHO:" + req.bodyString())
        }
        req_ipc.close()

        req_ipc.stream.waitClosed()
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

    @Test
    fun testClose() = runBlocking {
        var t = 0.0;

        var po = PromiseOut<Unit>();
        assertEquals(0.0, t);
        println("start1")
        val stream = ReadableStream(onStart = { controller ->
            println("start2")
            launch {
                println("start3")
                try {

                    controller.enqueue(byteArrayOf(1))
                    controller.enqueue(byteArrayOf(2))
                    controller.close()
                    try {
                        for (chunk in controller.stream) {
                            t += chunk.size
                        }
                    } finally {
                        t += 0.1
                    }

                    po.resolve(Unit);
                } catch (e: Exception) {
                    po.reject(e)
                }

            }
        });

        po.waitPromise()
        assertEquals(2.1, t);
    }
}