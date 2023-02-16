package info.bagen.rust.plaoc.microService.ipc

import android.content.res.Resources.NotFoundException
import info.bagen.rust.plaoc.microService.ipc.helper.IPC_DATA_TYPE
import info.bagen.rust.plaoc.microService.ipc.helper.IpcMessage
import java.io.InputStream

typealias Uint8Array = ByteArray

class RawDataToBody(val rawBody: RawData, var ipc: Ipc?) {
    var body: Any? = null
    val rawBodyType = rawBody.type
    val bodyEncoder = when (rawBody) {
        IPC_RAW_BODY_TYPE.TEXT -> textFactory()
        IPC_RAW_BODY_TYPE.BASE64 -> base64Factory()
        IPC_RAW_BODY_TYPE.BINARY ->binaryFactory()
        IPC_RAW_BODY_TYPE.STREAM_ID ->streamFactory()
        else -> {
            throw NotFoundException(rawBodyType.toString())
        }
    }
    /** 处理 binary*/
    private fun streamFactory() {
        if (ipc == null) {
            throw  Error("miss ipc when ipc-response has stream-body");
        }
        val streamIpc = ipc!!;
        val streamId = rawBody.value

        streamIpc.onMessage.listen { args ->
            val message = args[0] as IpcStreamData
            if (message.stream_id.isNotEmpty() && message.stream_id == streamId) {
                if (message.type == IPC_DATA_TYPE.STREAM_DATA) {

                } else if (message.type === IPC_DATA_TYPE.STREAM_END) {

                }
            }
        }
    }

    /** 处理 binary*/
    private fun binaryFactory() {

    }

    /** 处理 text*/
    private fun textFactory() {

    }
    /** 处理base64*/
    private fun base64Factory() {
    }

    /** 定义一个消耗函数 执行结果并且返回true*/
    inline fun consume(f: () -> Unit): Boolean {
        f()
        return true
    }
}
