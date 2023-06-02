package info.bagen.dwebbrowser.microService.core.ipc

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import java.io.InputStream

class IpcRequest(
    val req_id: Int,
    val url: String,
    val method: IpcMethod,
    val headers: IpcHeaders,
    val body: IpcBody,
    val ipc: info.bagen.dwebbrowser.microService.core.ipc.Ipc,
) : IpcMessage(IPC_MESSAGE_TYPE.REQUEST) {
    val uri by lazy { Uri.of(url) }

    init {
        if (body is IpcBodySender) {
          IpcBodySender.IPC.usableByIpc(ipc, body)
        }
    }

    companion object {

        fun fromText(
          req_id: Int,
          url: String,
          method: IpcMethod = IpcMethod.GET,
          headers: IpcHeaders = IpcHeaders(),
          text: String,
          ipc: info.bagen.dwebbrowser.microService.core.ipc.Ipc
        ) = IpcRequest(
            req_id,
            url,
            method,
            headers,// 这里 content-length 默认不写，因为这是要算二进制的长度，我们这里只有在字符串的长度，不是一个东西
            IpcBodySender.fromText(text, ipc),
            ipc,
        );

        fun fromBinary(
          req_id: Int,
          method: IpcMethod,
          url: String,
          headers: IpcHeaders = IpcHeaders(),
          binary: ByteArray,
          ipc: info.bagen.dwebbrowser.microService.core.ipc.Ipc
        ) = IpcRequest(
            req_id,
            url,
            method,
            headers.also {
                headers.init("Content-Type", "application/octet-stream");
                headers.init("Content-Length", binary.size.toString());
            },
            IpcBodySender.fromBinary(binary, ipc),
            ipc,
        )

        fun fromStream(
            req_id: Int,
            method: IpcMethod,
            url: String,
            headers: IpcHeaders = IpcHeaders(),
            stream: InputStream,
            ipc: info.bagen.dwebbrowser.microService.core.ipc.Ipc,
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
            IpcBodySender.fromStream(stream, ipc),
            ipc,
        )

        fun fromRequest(
            req_id: Int, request: Request, ipc: info.bagen.dwebbrowser.microService.core.ipc.Ipc
        ) = IpcRequest(
            req_id,
            request.uri.toString(),
            IpcMethod.from(request.method),
            IpcHeaders(request.headers),
            if (request.method == Method.GET || request.method == Method.HEAD) {
                IpcBodySender.fromText("", ipc)
            } else when (request.body.length) {
                0L -> IpcBodySender.fromText("", ipc)
                null -> IpcBodySender.fromStream(request.body.stream, ipc)
                else -> IpcBodySender.fromBinary(request.body.payload.array(), ipc)
            },
            ipc,
        )

    }

    fun toRequest() = Request(method.http4kMethod, url).headers(headers.toList()).let { req ->
        if (req.method == Method.GET || req.method == Method.HEAD) {
            req
        } else when (val body = body.raw) {
            is String -> req.body(body)
            is ByteArray -> req.body(body.inputStream(), body.size.toLong())
            is InputStream -> req.body(body)
            else -> throw Exception("invalid body to request: $body")
        }
    }

    val ipcReqMessage by lazy {
        IpcReqMessage(req_id, method, url, headers.toMap(), body.metaBody)
    }

    override fun toString() = "#IpcRequest/$method/$url"
}

class IpcReqMessage(
  val req_id: Int,
  val method: IpcMethod,
  val url: String,
  val headers: MutableMap<String, String>,
  val metaBody: MetaBody,
) : IpcMessage(IPC_MESSAGE_TYPE.REQUEST)
