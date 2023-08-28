package org.microservice

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dweb_browser.helper.readByteArray
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.text
import org.dweb_browser.microservice.ipc.NativeIpc
import org.dweb_browser.microservice.ipc.NativeMessageChannel
import org.dweb_browser.microservice.ipc.ReadableStreamIpc
import org.dweb_browser.microservice.ipc.helper.IPC_ROLE
import org.dweb_browser.microservice.ipc.helper.IpcHeaders
import org.dweb_browser.microservice.ipc.helper.IpcMessage
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.dweb_browser.microservice.ipc.helper.ReadableStream
import org.dweb_browser.microservice.ipc.helper.debugStream
import org.dweb_browser.microservice.sys.dns.DnsNMM
import org.dweb_browser.microservice.sys.dns.nativeFetch
import org.dweb_browser.microservice.sys.dns.nativeFetchAdaptersManager
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NativeIpcTest : AsyncBase() {
  @Test
  fun baseTest() = runBlocking {
    val m1 = object : NativeMicroModule("m1", "") {
      override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
      }

      override suspend fun _shutdown() {
      }
    }
    val m2 = object : NativeMicroModule("m2", "") {
      override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
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
          req.req_id, 200, IpcHeaders(), "ECHO:" + req.body.text(), ipc
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

  @Test
  @ExperimentalCoroutinesApi
  fun sendStreamData() = runBlocking(catcher) {
    enableDwebDebug(listOf("native-ipc", "stream"))
    val dns = DnsNMM()

    val m0 = object : NativeMicroModule("m0", "") {
      override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
      }

      override suspend fun _shutdown() {
      }
    }
    val m1 = object : NativeMicroModule("m1", "") {
      override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
      }

      override suspend fun _shutdown() {
      }
    }
    dns.bootstrapMicroModule(m0)
    dns.bootstrapMicroModule(m1)


    val channel = NativeMessageChannel<IpcMessage, IpcMessage>();
    val ipc0 = NativeIpc(channel.port1, m0, IPC_ROLE.SERVER);
    val ipc1 = NativeIpc(channel.port2, m1, IPC_ROLE.CLIENT);

    ipc0.onRequest { (req, ipc) ->
//            delay(200)
      val req_stream = req.body.stream()
      val res_stream = ReadableStream(onStart = { controller ->
        /// 这里不可以用上下文所使用的launch，而是要重新开一个，以确保和 postMessage-fromStream 所使用的分开
        launch {
          delay(100)
          debugStream("PIPE/START", "$req_stream >>> ${controller.stream}")
          while (true) {
            val byteLen = req_stream.available() // TODO 这里过一段时间会自己关闭，打个断点能发现这个行为
            debugStream(
              "PIPE/ON-DATA", "$req_stream >> $byteLen >> ${controller.stream}"
            )
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
          req.req_id, 200, IpcHeaders(), res_stream, ipc
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
        controller.enqueue(chunk.toByteArray())
      }
      controller.close()
    }

    val res = ipc1.request(Request(Method.GET, "").body(stream))
    println("got res")
    assertEquals(res.text(), body)
    println("got res.body: ${res.text()}")
    ipc0.close()
    ipc1.close()
    m0.shutdown()
    m1.shutdown()

    delay(1000)
    printDumpCoroutinesInfo()
  }

  @Test
  fun withReadableStreamIpc() = runBlocking {
//        System.setProperty("dweb-debug", "stream native-ipc")
    val dnsNMM = DnsNMM()
    val mServer = object : NativeMicroModule("mServer", "") {
      override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        apiRouting = routes("/listen" bind Method.POST to defineHandler { request, ipc ->
          val streamIpc = ReadableStreamIpc(ipc.remote, "from-remote")
          println("SERVER STREAM-IPC/STREAM: ${streamIpc.stream}")
          streamIpc.bindIncomeStream(request.body.stream)
          streamIpc.onRequest { (request, ipc) ->
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
          Response(Status.OK).body(streamIpc.stream)
        })
      }

      override suspend fun _shutdown() {
        apiRouting = null
      }
    }

    val mClient = object : NativeMicroModule("mClient", "") {
      override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
      }

      override suspend fun _shutdown() {
      }
    }

    val c2sIpc by lazy { runBlocking { mServer.connect(mClient.mmid)!! } }

    nativeFetchAdaptersManager.append { _, request ->
      if (request.uri.host == mServer.mmid) {
        c2sIpc.request(request)
      } else null
    }

    dnsNMM.bootstrapMicroModule(mServer)
    dnsNMM.bootstrapMicroModule(mClient)


    val clientStreamIpc = ReadableStreamIpc(mClient, "as-remote")
    println("CLIENT STREAM-IPC/STREAM: ${clientStreamIpc.stream}")

    val res = dnsNMM.nativeFetch(
      Request(Method.POST, "http://mServer/listen").body(clientStreamIpc.stream)
    )
    clientStreamIpc.bindIncomeStream(res.body.stream);


    delay(1000)
    for (i in 0..10) {
      println("开始发送 $i")
      val request = Request(Method.GET, "").body("hi-$i")
      val response = clientStreamIpc.request(request)
      assertEquals(response.text(), "ECHO:" + request.bodyString())
      println("测试通过 $i: ${response.text()}")
    }

    clientStreamIpc.close()

    clientStreamIpc.stream.waitClosed()

    delay(1000)
    printDumpCoroutinesInfo()

  }

  @Test
  fun IpcOnClose() = runBlocking {
    val m1 = object : NativeMicroModule("m1", "") {
      override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
      }

      override suspend fun _shutdown() {
      }
    }
    val m2 = object : NativeMicroModule("m2", "") {
      override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
      }

      override suspend fun _shutdown() {
      }
    }
    val channel = NativeMessageChannel<IpcMessage, IpcMessage>();
    val ipc1 = NativeIpc(channel.port1, m1, IPC_ROLE.SERVER);
    val ipc2 = NativeIpc(channel.port2, m2, IPC_ROLE.CLIENT);
    var t = 0;
    ipc1.onClose {
      t += 1
      println("closed ${ipc1.remote.mmid} ")
    }
    ipc2.onClose {
      t += 1
      println("closed ${ipc2.remote.mmid}")
    }

    ipc1.close()
    assertEquals(2, t)
  }
}