package info.bagen.dwebbrowser.learn

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dweb_browser.microservice.ipc.helper.ReadableStream
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.lens.Query
import org.http4k.lens.string

fun main() {
    val stream = ReadableStream(onStart = { controller ->
        GlobalScope.launch {
            var i = 5
            while (i-- > 0) {
                controller.enqueue(byteArrayOf(i.toByte()))
                Thread.sleep(400)
            }
            controller.close()
        }
    });

    val req = Request(Method.POST, Uri.of( "/listen").query("data","xxxi")).body(stream)

    val query_data = Query.string().required("data")

    val app: HttpHandler = { request ->
        println("data: ${query_data(request)}")
        val bodyStream = request.body.stream
        while (bodyStream.available() > 0) {
            println("body read: ${bodyStream.read()}")
        }
        Response(Status.OK).body("OKK!")
    }

    runBlocking {
        app(req)
    }
}