package info.bagen.rust.plaoc

import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Signal
import info.bagen.rust.plaoc.microService.helper.text
import info.bagen.rust.plaoc.microService.helper.toByteArray
import info.bagen.rust.plaoc.microService.ipc.IPC_ROLE
import info.bagen.rust.plaoc.microService.ipc.IpcHeaders
import info.bagen.rust.plaoc.microService.ipc.IpcResponse
import info.bagen.rust.plaoc.microService.ipc.ReadableStream
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.ReadableStreamIpc
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.http4k.core.Method
import org.http4k.core.Request
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ReadableStreamTest {
    @Test
    fun test1() = runBlocking {
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
            override suspend fun _bootstrap() {
                TODO("Not yet implemented")
            }

            override suspend fun _shutdown() {
                TODO("Not yet implemented")
            }
        }


        val m2 = object : NativeMicroModule("m2") {
            override suspend fun _bootstrap() {
                TODO("Not yet implemented")
            }

            override suspend fun _shutdown() {
                TODO("Not yet implemented")
            }
        }

        val req_ipc = ReadableStreamIpc(m1, IPC_ROLE.CLIENT)
        val res_ipc = ReadableStreamIpc(m2, IPC_ROLE.SERVER)
        req_ipc.bindIncomeStream(res_ipc.stream, "from-res")
        res_ipc.bindIncomeStream(req_ipc.stream, "from-req")


        GlobalScope.launch {
            res_ipc.onRequest { (request, ipc) ->
                println("req get request $request")
                delay(100)
                println("echo after 1s $request")
                ipc.postMessage(
                    IpcResponse.fromText(
                        request.req_id,
                        200,
                        "ECHO:" + request.text(),
                        IpcHeaders(),
                        ipc
                    )
                )
            }
        }

        GlobalScope.launch {
            for (i in 1..10) {
                println("开始发送 $i")
                val req = Request(Method.GET, "").body("hi-$i")
                val res = req_ipc.request(req)
                assertEquals(res.text(), "ECHO:" + req.bodyString())
            }
            req_ipc.close()
        }

        req_ipc.stream.closed()
    }
}