package info.bagen.rust.plaoc

import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.asUtf8
import info.bagen.rust.plaoc.microService.helper.readByteArray
import info.bagen.rust.plaoc.microService.helper.text
import info.bagen.rust.plaoc.microService.ipc.*
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.ReadableStreamIpc
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetchAdaptersManager
import kotlinx.coroutines.*
import kotlinx.coroutines.debug.DebugProbes
import org.http4k.core.*
import org.http4k.core.HttpMessage.Companion.HTTP_2
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NativeIpcTest {
    @Test
    fun baseTest() = runBlocking {
        val m1 = object : NativeMicroModule("m1") {
            override suspend fun _bootstrap() {
            }

            override suspend fun _shutdown() {
            }
        }
        val m2 = object : NativeMicroModule("m2") {
            override suspend fun _bootstrap() {
            }

            override suspend fun _shutdown() {
            }
        }

        val channel = NativeMessageChannel<IpcMessage, IpcMessage>();
        val ipc1 = NativeIpc(channel.port1, m1, IPC_ROLE.SERVER);
        val ipc2 = NativeIpc(channel.port2, m2, IPC_ROLE.CLIENT);

        ipc1.onRequest { (req, ipc) ->
            delay(200)
            ipc.postMessage(
                IpcResponse.fromText(
                    req.req_id, 200, "ECHO:" + req.text(), IpcHeaders(), ipc
                )
            )
        }


        delay(100)
        for (i in 1..10) {
            println("开始发送 $i")
            val req = Request(Method.GET, "").body("hi-$i")
            val res = ipc2.request(req)
            assertEquals(res.text(), "ECHO:" + req.bodyString())
        }
        ipc2.close()
    }

    private val catcher = CoroutineExceptionHandler { ctx, e ->
        e.printStackTrace()
    }

    @Test
    @ExperimentalCoroutinesApi
    fun sendStreamData() = runBlocking(catcher) {
        DebugProbes.install()

        System.setProperty("dweb-debug", "native-ipc stream")
        val m1 = object : NativeMicroModule("m1") {
            override suspend fun _bootstrap() {
            }

            override suspend fun _shutdown() {
            }
        }
        val m2 = object : NativeMicroModule("m2") {
            override suspend fun _bootstrap() {
            }

            override suspend fun _shutdown() {
            }
        }
        m1.bootstrap()
        m2.bootstrap()


        val channel = NativeMessageChannel<IpcMessage, IpcMessage>();
        val ipc1 = NativeIpc(channel.port1, m1, IPC_ROLE.SERVER);
        val ipc2 = NativeIpc(channel.port2, m2, IPC_ROLE.CLIENT);

        ipc1.onRequest { (req, ipc) ->
            delay(200)
            val req_stream = req.stream()
            val res_stream = ReadableStream(onStart = { controller ->
                launch {
                    debugStream("PIPE/START", "$req_stream >>> ${controller.stream}")
                    while (true) {
                        val byteLen = req_stream.available()
                        println("available byte length: $byteLen")
                        if (byteLen > 0) {
                            controller.enqueue(req_stream.readByteArray(byteLen))
                        } else break
                    }
                    debugStream("PIPE/END", "$req_stream >>> ${controller.stream}")
                    controller.close()
                }
            })
            ipc.postMessage(
                IpcResponse.fromStream(
                    req.req_id, 200, res_stream, IpcHeaders(), ipc
                )
            )
        }

        lateinit var controller: ReadableStream.ReadableStreamController
        val stream = ReadableStream(onStart = {
            controller = it
        }, onPull = { (desiredSize, controller) ->
            println("收到数据拉取请求 ${controller.stream} $desiredSize")
        });
        var body = ""
        launch {
            delay(100)
            for (i in 1..10) {
                delay(200)
                println("开始发送 $i")
                val chunk = "[$i]"
                body += chunk
                controller.enqueue(chunk.asUtf8())
            }
            controller.close()
        }

        val res = ipc2.request(Request(Method.GET, "").body(stream))
        println("got res")
        assertEquals(res.text(), body)
        println("got res.body: ${res.text()}")
        ipc1.close()
        ipc2.close()
        m1.shutdown()
        m2.shutdown()


        delay(1000)
        var i = 1
        println("job.isCompleted: \n${
            DebugProbes.dumpCoroutinesInfo().joinToString(separator = "\n") { it ->
                "${i++}.\t| $it"
            }
        }")

    }

    @Test
    fun withReadableStream() = runBlocking {
        System.setProperty("dweb-debug", "native native-ipc")

        val mServer = object : NativeMicroModule("mServer") {
            override suspend fun _bootstrap() {
                apiRouting = routes("/listen" bind Method.POST to defineHandler { request, ipc ->
                    val streamIpc = ReadableStreamIpc(ipc.remote, IPC_ROLE.SERVER)
                    println("SERVER STREAM-IPC/STREAM: ${streamIpc.stream}")
                    streamIpc.bindIncomeStream(request.body.stream, "from-remote")
                    streamIpc.onRequest { (request, ipc) ->
                        println("req get request $request")
                        delay(200)
                        println("echo after 1s $request")
                        ipc.postMessage(
                            IpcResponse.fromText(
                                request.req_id, 200, "ECHO:" + request.text(), IpcHeaders(), ipc
                            )
                        )
                    }
                    Response(Status.OK).body(streamIpc.stream)
                })
            }

            override suspend fun _shutdown() {
                apiRouting = null
            }
        }


        val mClient = object : NativeMicroModule("mClient") {
            override suspend fun _bootstrap() {
            }

            override suspend fun _shutdown() {
            }
        }

        val c2sIpc by lazy { runBlocking { mServer.connect(mClient) } }

        nativeFetchAdaptersManager.append { mm, request ->
            if (request.uri.host == mServer.mmid) {
                c2sIpc.request(request)
            } else null
        }
        mServer.bootstrap()
        mClient.bootstrap()


        val clientStreamIpc = ReadableStreamIpc(mClient, IPC_ROLE.CLIENT)
        println("CLIENT STREAM-IPC/STREAM: ${clientStreamIpc.stream}")

        val req_body = StreamBody(
            clientStreamIpc.stream
        )
        val res = mClient.nativeFetch(
            Request(Method.POST, "http://mServer/listen", HTTP_2).body(
                req_body
            )
        )
        clientStreamIpc.bindIncomeStream(res.body.stream, "as-remote");


        delay(1000)
        for (i in 0..10) {
            println("开始发送 $i")
            val req = Request(Method.GET, "").body("hi-$i")
            val res = clientStreamIpc.request(req)
            assertEquals(res.text(), "ECHO:" + req.bodyString())
        }

        clientStreamIpc.close()

        clientStreamIpc.stream.afterClosed()
    }
}