package info.bagen.dwebbrowser.microService.sys.http.net

import io.ktor.http.*
import io.ktor.http.ContentType
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.withContext
import org.http4k.core.*
import org.http4k.core.Headers
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.server.supportedOrNull
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit.SECONDS
import io.ktor.http.Headers as KHeaders

@Suppress("EXPERIMENTAL_API_USAGE")
class MyKtorCIO(val port: Int = 8000, override val stopMode: ServerConfig.StopMode) : ServerConfig {
    constructor(port: Int = 8000) : this(port, Immediate)

    init {
        if (stopMode != Immediate) {
            throw ServerConfig.UnsupportedStopMode(stopMode)
        }
    }

    override fun toServer(http: HttpHandler): Http4kServer = object : Http4kServer {
        private val engine: CIOApplicationEngine = embeddedServer(CIO, port) {
            install(createApplicationPlugin(name = "http4k") {
                onCall {
                    withContext(Default) {
                        it.response.fromHttp4K(
                            it.request.asHttp4k()?.let(http) ?: Response(
                                NOT_IMPLEMENTED
                            )
                        )
                    }
                }
            })
        }

        override fun start() = apply {
            engine.start()
        }

        override fun stop() = apply {
            engine.stop(0, 2, SECONDS)
        }

        override fun port() = engine.environment.connectors[0].port
    }
}

fun ApplicationRequest.asHttp4k() = Method.supportedOrNull(httpMethod.value)?.let {
    Request(it, uri)
        .headers(headers.toHttp4kHeaders())
        .body(receiveChannel().toInputStream(), header("Content-Length")?.toLong())
        .source(
            RequestSource(
                origin.remoteHost,
                scheme = origin.scheme
            )
        ) // origin.remotePort does not exist for Ktor
}

suspend fun ApplicationResponse.fromHttp4K(response: Response) {
    status(HttpStatusCode.fromValue(response.status.code))
    response.headers
        .filterNot { HttpHeaders.isUnsafe(it.first) || it.first == CONTENT_TYPE.meta.name }
        .forEach { header(it.first, it.second ?: "") }
    call.respondOutputStream(
        CONTENT_TYPE(response)?.let { ContentType.parse(it.toHeaderValue()) }
    ) {
        response.body.stream.copyToWithFlush(this)
    }
}

private fun InputStream.copyToWithFlush(
    out: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE
): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    try {
        var bytes = read(buffer)
        while (bytes >= 0) {
            out.write(buffer, 0, bytes)
            out.flush()
            bytesCopied += bytes
            bytes = read(buffer)
        }
    } catch (e: Exception) {
        close()
        throw e
    }
    return bytesCopied
}

private fun KHeaders.toHttp4kHeaders(): Headers = names().flatMap { name ->
    (getAll(name) ?: emptyList()).map { name to it }
}
