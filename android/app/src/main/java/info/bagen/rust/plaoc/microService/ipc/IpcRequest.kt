package info.bagen.rust.plaoc.microService.ipc

import org.http4k.core.Request
import info.bagen.rust.plaoc.microService.helper.Method
import info.bagen.rust.plaoc.microService.helper.toBase64
import org.http4k.core.Uri
import java.io.InputStream

data class IpcRequest(
    val req_id: Int,
    val method: Method,
    val url: String,
    val headers: IpcHeaders,
    override val rawBody: RawData,
    override val ipc: Ipc
) : IpcBody(rawBody, ipc), IpcMessage {
    override val type = IPC_DATA_TYPE.REQUEST

    val uri by lazy { Uri.of(url) }

    companion object {

        fun fromRequest(req_id: Int, request: Request, ipc: Ipc) =
            if (request.body.length === null)
                IpcRequest.fromStream(
                    req_id,
                    Method.from(request.method),
                    request.uri.toString(),
                    IpcHeaders(request.headers),
                    request.body.stream,
                    ipc
                )
            else if (request.body.length === 0L)
                IpcRequest.fromText(
                    req_id,
                    Method.from(request.method),
                    request.uri.toString(),
                    IpcHeaders(request.headers),
                    "", ipc,
                )
            else
                IpcRequest.fromBinary(
                    req_id,
                    Method.from(request.method),
                    request.uri.toString(),
                    IpcHeaders(request.headers),
                    request.body.payload.array(),
                    ipc
                )


        fun fromText(
            req_id: Int,
            method: Method,
            url: String,
            headers: IpcHeaders = IpcHeaders(),
            text: String,
            ipc: Ipc
        ) = IpcRequest(
            req_id,
            method,
            url,
            // 这里 content-length 默认不写，因为这是要算二进制的长度，我们这里只有在字符串的长度，不是一个东西
            headers,
            RawData(IPC_RAW_BODY_TYPE.TEXT, text),
            ipc
        );

        fun fromBinary(
            req_id: Int,
            method: Method,
            url: String,
            headers: IpcHeaders = IpcHeaders(),
            binary: ByteArray,
            ipc: Ipc
        ) = IpcRequest(
            req_id,
            method,
            url,
            // 这里 content-length 默认不写，因为这是要算二进制的长度，我们这里只有在字符串的长度，不是一个东西
            headers.also {
                headers.init("Content-Type", "application/octet-stream");
                headers.init("Content-Length", binary.size.toString());
            },
            if (ipc.supportMessagePack)
                RawData(IPC_RAW_BODY_TYPE.BINARY, binary)
            else RawData(
                IPC_RAW_BODY_TYPE.BASE64,
                binary.toBase64()
            ),
            ipc
        )

        fun fromStream(
            req_id: Int,
            method: Method,
            url: String,
            headers: IpcHeaders = IpcHeaders(),
            stream: InputStream,
            ipc: Ipc,
            size: Long? = null
        ) = IpcRequest(
            req_id,
            method,
            url,
            // 这里 content-length 默认不写，因为这是要算二进制的长度，我们这里只有在字符串的长度，不是一个东西
            headers.also {
                headers.init("Content-Type", "application/octet-stream");
                if (size !== null) {
                    headers.init("Content-Length", size.toString());
                }
            },
            "res/$req_id/${headers.getOrDefault("Content-Length", "-")}".let { stream_id ->
                streamAsRawData(stream_id, stream, ipc);
                if (ipc.supportMessagePack)
                    RawData(IPC_RAW_BODY_TYPE.BINARY_STREAM_ID, stream_id)
                else RawData(
                    IPC_RAW_BODY_TYPE.BASE64_STREAM_ID,
                    stream_id
                )
            },
            ipc
        )
    }

    fun asRequest() = Request(method.http4kMethod, url).headers(headers.toList()).body(stream())
}
