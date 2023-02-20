package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.*
import org.http4k.core.Response
import org.http4k.core.Status
import java.io.InputStream

class IpcResponse(
    req_id: Int,
    statusCode: Int,
    headers: IpcHeaders,
    rawBody: RawData,
    override val ipc: Ipc
) : IpcResponseData(
    req_id,
    statusCode,
    headers,
    rawBody
) {

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
        ): IpcResponse {
            headers.init("Content-Type", "application/octet-stream");
            val stream_id = "res/${req_id}/${headers.get("Content-Length") ?: "-"}";
            val ipcResponse = IpcResponse(
                req_id,
                statusCode,
                headers,
                if (ipc.supportBinary) {
                    RawData(IPC_RAW_BODY_TYPE.BINARY_STREAM_ID, stream_id)
                } else {
                    RawData(IPC_RAW_BODY_TYPE.BASE64_STREAM_ID, stream_id)
                },
                ipc
            );
            streamAsRawData(stream_id, stream, ipc);
            return ipcResponse;
        }

        fun fromResponse(
            req_id: Int,
            response: Response,
            ipc: Ipc
        ): IpcResponse {
            return fromStream(
                req_id,
                response.status.code,
                response.body.stream,
                IpcHeaders(response.headers),
                ipc
            )
        }
    }

    fun asResponse() =
        Response(Status(this.statusCode, null))
            .headers(this.headers.toList()).also { res ->
                when (body) {
                    is String -> res.body(body)
                    is ByteArray -> res.body(body.inputStream(), body.size.toLong())
                    is InputStream -> res.body(body)
                    else -> throw Exception("invalid body to response: $body")
                }
            }
}

abstract class IpcResponseData(
    val req_id: Int,
    val statusCode: Int,
    val headers: IpcHeaders,
    override val rawBody: RawData,
) : IpcBody(), IpcMessage {
    override val type = IPC_DATA_TYPE.RESPONSE
}