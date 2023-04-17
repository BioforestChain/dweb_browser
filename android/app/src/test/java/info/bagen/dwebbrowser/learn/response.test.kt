package info.bagen.rust.dwebbrowser.microService.ipc

import org.http4k.asByteBuffer
import org.http4k.core.MemoryBody
import org.http4k.core.Response
import org.http4k.core.Status

fun main() {
    Response(Status.OK).body("你好").logBody()

    Response(Status.OK).body(MemoryBody(byteArrayOf(1, 2, 3).asByteBuffer())).logBody()

    byteArrayOf(1, 2, 3).also { body ->
        Response(Status.OK).body(body.inputStream(), body.size.toLong()).logBody()
    }

    byteArrayOf(1, 2, 3).also { body ->
        Response(Status.OK).body(body.inputStream()).logBody()
    }

}

inline fun Response.logBody() {
    println("length: ${body.length} data:${body.payload.array().toList()}")
}