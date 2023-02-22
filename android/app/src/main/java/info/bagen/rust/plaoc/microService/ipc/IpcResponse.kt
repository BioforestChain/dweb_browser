package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.helper.toBase64
import org.http4k.core.Response
import org.http4k.core.Status
import java.io.InputStream

class IpcResponse(
    val req_id: Int,
    val statusCode: Int,
    val headers: IpcHeaders,
    rawBody: RawData,
    ipc: Ipc
) : IpcBody(IPC_DATA_TYPE.RESPONSE, rawBody, ipc) {

    companion object {
        fun fromJson(
            req_id: Int,
            statusCode: Int,
            jsonAble: Any,
            headers: IpcHeaders = IpcHeaders(),
            ipc: Ipc
        ): IpcResponse {
            headers.init("Content-Type", "application/json")
            return fromText(req_id, statusCode, gson.toJson(jsonAble), headers, ipc)
        }

        fun fromText(
            req_id: Int,
            statusCode: Int,
            text: String,
            headers: IpcHeaders = IpcHeaders(),
            ipc: Ipc
        ): IpcResponse {
            headers.init("Content-Type", "text/plain")
            return IpcResponse(
                req_id,
                statusCode,
                headers,
                RawData(IPC_RAW_BODY_TYPE.TEXT, text),
                ipc
            )
        }

        fun fromBinary(
            req_id: Int,
            statusCode: Int,
            binary: ByteArray,
            headers: IpcHeaders,
            ipc: Ipc
        ): IpcResponse {
            headers.init("Content-Type", "application/octet-stream");
            headers.init("Content-Length", binary.size.toString());
            return IpcResponse(
                req_id,
                statusCode,
                headers,
                if (ipc.supportBinary) {
                    RawData(IPC_RAW_BODY_TYPE.BINARY, binary)
                } else {
                    RawData(IPC_RAW_BODY_TYPE.BASE64, binary.toBase64())
                },
                ipc
            );
        }

        fun fromStream(
            req_id: Int,
            statusCode: Int,
            stream: InputStream,
            headers: IpcHeaders,
            ipc: Ipc
        ) = IpcResponse(
            req_id,
            statusCode,
            headers.also {
                headers.init("Content-Type", "application/octet-stream");
            },
            streamAsRawData(stream, ipc),
            ipc
        )

        fun fromResponse(
            req_id: Int,
            response: Response,
            ipc: Ipc
        ) = when (response.body.length) {
            0L -> IpcResponse(
                req_id,
                response.status.code,
                IpcHeaders(response.headers),
                RawData(IPC_RAW_BODY_TYPE.TEXT, ""),
                ipc
            )
            null -> fromStream(
                req_id,
                response.status.code,
                response.body.stream,
                IpcHeaders(response.headers),
                ipc
            )
            else -> fromBinary(
                req_id,
                response.status.code,
                response.body.payload.array(),
                IpcHeaders(response.headers),
                ipc
            )
        }
    }

    fun asResponse() =
        Response(Status(this.statusCode, null))
            .headers(this.headers.toList()).let { res ->
                when (val body = body) {
                    is String -> res.body(body)
                    is ByteArray -> res.body(body.inputStream(), body.size.toLong())
                    is InputStream -> res.body(body)
                    else -> throw Exception("invalid body to response: $body")
                }
            }

    val data by lazy {
        IpcResponseData(req_id, statusCode, headers, rawBody)
    }
}

class IpcResponseData(
    val req_id: Int,
    val statusCode: Int,
    val headers: IpcHeaders,
    val rawBody: RawData,
) : IpcMessage(IPC_DATA_TYPE.RESPONSE)