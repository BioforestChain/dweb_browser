package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.*
import info.bagen.rust.plaoc.microService.network.gson
import io.ktor.client.request.*
import org.http4k.core.Response
import org.http4k.core.Status
import java.io.InputStream

data class IpcResponse(
    val req_id: Number = 0,
    val statusCode: Int = 200,
    val headers: IpcHeaders,
    override val rawBody: RawData,
    override val ipc: Ipc
) : IpcBody(rawBody, ipc), IpcMessage {
    override val type = IPC_DATA_TYPE.RESPONSE

    companion object {
        fun fromJson(
            req_id: Number,
            statusCode: Int,
            jsonAble: Any,
            headers: IpcHeaders = IpcHeaders(),
            ipc: Ipc
        ): IpcResponse {
            headers.init("Content-Type", "application/json")
            return fromText(req_id, statusCode, gson.toJson(jsonAble), headers, ipc)
        }

        fun fromText(
            req_id: Number,
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
            req_id: Number,
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
                if (ipc.supportMessagePack) {
                    RawData(IPC_RAW_BODY_TYPE.BINARY, binary)
                } else {
                    RawData(IPC_RAW_BODY_TYPE.BASE64, binary.toBase64())
                },
                ipc
            );
        }

        fun fromStream(
            req_id: Number,
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
                if (ipc.supportMessagePack) {
                    RawData(IPC_RAW_BODY_TYPE.BINARY_STREAM_ID, stream_id)
                } else {
                    RawData(IPC_RAW_BODY_TYPE.BASE64_STREAM_ID, stream_id)
                },
                ipc
            );
            streamAsRawData(stream_id, stream, ipc);
            return ipcResponse;
        }
    }

    suspend fun asResponse(): Response {
        return Response(Status(this.statusCode, null))
            .headers(this.headers.toList())
            .body(this.stream())
    }
}
