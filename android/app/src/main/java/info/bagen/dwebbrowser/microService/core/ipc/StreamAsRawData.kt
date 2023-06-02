package info.bagen.dwebbrowser.microService.core.ipc

import java.io.InputStream

/**
 * 如果 rawData 是流模式，需要提供数据发送服务
 *
 * 这里不会一直无脑发，而是对方有需要的时候才发
 * @param stream_id
 * @param stream
 * @param ipc
 */
class StreamAsRawData(
    val stream_id: String,
    val stream: InputStream,
    ipc: Ipc
) {
//    stream
}