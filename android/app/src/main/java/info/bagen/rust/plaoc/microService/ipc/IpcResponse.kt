package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.gson
import org.http4k.core.Response
import org.http4k.core.Status
import java.io.InputStream

class IpcResponse(
    val req_id: Int,
    val statusCode: Int,
    val headers: IpcHeaders,
    val body: IpcBody,
    val ipc: Ipc,
) : IpcMessage(IPC_MESSAGE_TYPE.RESPONSE) {

    init {
        if (body is IpcBodySender) {
            IpcBodySender.usableByIpc(ipc, body)
        }
    }

    companion object {
        fun fromJson(
            req_id: Int,
            statusCode: Int = 200,
            headers: IpcHeaders = IpcHeaders(),
            jsonAble: Any,
            ipc: Ipc
        ) = fromText(req_id, statusCode, headers.also {
            headers.init("Content-Type", "application/json")
        }, gson.toJson(jsonAble), ipc)

        fun fromText(
            req_id: Int,
            statusCode: Int = 200,
            headers: IpcHeaders = IpcHeaders(),
            text: String,
            ipc: Ipc
        ) = IpcResponse(
            req_id,
            statusCode,
            headers.also { headers.init("Content-Type", "text/plain") },
            IpcBodySender.from(text, ipc),
            ipc,
        )

        fun fromBinary(
            req_id: Int, statusCode: Int = 200, headers: IpcHeaders, binary: ByteArray, ipc: Ipc
        ) = IpcResponse(
            req_id,
            statusCode,
            headers.also {
                headers.init("Content-Type", "application/octet-stream");
                headers.init("Content-Length", binary.size.toString());
            },
            IpcBodySender.from(binary, ipc),
            ipc,
        );


        fun fromStream(
            req_id: Int,
            statusCode: Int = 200,
            headers: IpcHeaders = IpcHeaders(),
            stream: InputStream,
            ipc: Ipc
        ) = IpcResponse(
            req_id,
            statusCode,
            headers.also {
                headers.init("Content-Type", "application/octet-stream");
            },
            IpcBodySender.from(stream, ipc),
            ipc,
        )

        fun fromResponse(
            req_id: Int, response: Response, ipc: Ipc
        ) = IpcResponse(
            req_id,
            response.status.code,
            IpcHeaders(response.headers),
            when (response.body.length) {
                0L -> IpcBodySender.from("", ipc)
                null -> IpcBodySender.from(response.body.stream, ipc)
                else -> IpcBodySender.from(response.body.payload.array(), ipc)
            },
            ipc,
        )
    }

    fun toResponse() =
        Response(Status(this.statusCode, null)).headers(this.headers.toList()).let { res ->
            when (val body = body.raw) {
                is String -> res.body(body)
                is ByteArray -> res.body(body.inputStream(), body.size.toLong())
                is InputStream -> res.body(body)
                else -> throw Exception("invalid body to response: $body")
            }
        }

    val ipcResMessage by lazy {
        IpcResMessage(req_id, statusCode, headers.toMap(), body.metaBody)
    }
}

class IpcResMessage(
    val req_id: Int,
    val statusCode: Int,
    val headers: MutableMap<String, String>,
    val metaBody: MetaBody,
) : IpcMessage(IPC_MESSAGE_TYPE.RESPONSE)