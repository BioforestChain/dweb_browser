package info.bagen.rust.plaoc.microService.ipc

import org.http4k.core.Request
import org.http4k.core.Uri
import java.io.InputStream

class IpcRequest(
    val req_id: Int,
    val url: String,
    val method: IpcMethod,
    val headers: IpcHeaders,
    val body: IpcBody,
) : IpcMessage(IPC_DATA_TYPE.REQUEST) {
    val uri by lazy { Uri.of(url) }

    companion object {

        fun fromText(
            req_id: Int,
            url: String,
            method: IpcMethod = IpcMethod.GET,
            headers: IpcHeaders = IpcHeaders(),
            text: String,
            ipc: Ipc
        ) = IpcRequest(
            req_id,
            url,
            method,
            headers,// 这里 content-length 默认不写，因为这是要算二进制的长度，我们这里只有在字符串的长度，不是一个东西
            IpcBodySender(text, ipc)
        );

        fun fromBinary(
            req_id: Int,
            method: IpcMethod,
            url: String,
            headers: IpcHeaders = IpcHeaders(),
            binary: ByteArray,
            ipc: Ipc
        ) = IpcRequest(
            req_id,
            url,
            method,
            headers.also {
                headers.init("Content-Type", "application/octet-stream");
                headers.init("Content-Length", binary.size.toString());
            },
            IpcBodySender(binary, ipc)
        )

        fun fromStream(
            req_id: Int,
            method: IpcMethod,
            url: String,
            headers: IpcHeaders = IpcHeaders(),
            stream: InputStream,
            ipc: Ipc,
            size: Long? = null
        ) = IpcRequest(
            req_id,
            url,
            method,
            headers.also {
                headers.init("Content-Type", "application/octet-stream");
                if (size !== null) {
                    headers.init("Content-Length", size.toString());
                }
            },
            IpcBodySender(stream, ipc),
        )

        fun fromRequest(
            req_id: Int,
            request: Request,
            ipc: Ipc
        ) = IpcRequest(
            req_id,
            request.uri.toString(),
            IpcMethod.from(request.method),
            IpcHeaders(request.headers),
            when (request.body.length) {
                0L -> IpcBodySender("", ipc)
                null -> IpcBodySender(request.body.stream, ipc)
                else -> IpcBodySender(request.body.payload.array(), ipc)
            }
        )

    }

    fun asRequest() = Request(method.http4kMethod, url).headers(headers.toList()).let { req ->
        when (val body = body.body) {
            is String -> req.body(body)
            is ByteArray -> req.body(body.inputStream(), body.size.toLong())
            is InputStream -> req.body(body)
            else -> throw Exception("invalid body to request: $body")
        }
    }

    val ipcReqMessage by lazy {
        IpcReqMessage(req_id, method, url, headers, body.metaBody)
    }
}

class IpcReqMessage(
    val req_id: Int,
    val method: IpcMethod,
    val url: String,
    val headers: IpcHeaders,
    val metaBody: MetaBody,
) : IpcMessage(IPC_DATA_TYPE.REQUEST)
