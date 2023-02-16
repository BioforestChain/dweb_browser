package info.bagen.rust.plaoc.microService.ipc

import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

fun streamAsRawData(
    stream_id: String,
    stream: InputStream,
    ipc: Ipc
) {
    val channel = stream.toByteReadChannel()
//    channel.read {
//
//    }
//
//    ipc.onRequest()

}

/**
 * @return string|ByteArray|InputStream
 */
fun rawDataToBody(rawBody: RawData, ipc: Ipc): Any {
    if (rawBody.type and IPC_RAW_BODY_TYPE.STREAM_ID !== 0) {
        val stream_id = rawBody.data as String;
        val stream = ByteChannel();
//        stream.writeWhile { block->
//
//        }
//        stream.onReady {
//            ipc.postMessage(PULL)
//        }
        ipc.onMessage { message, ipc ->
            if (message is IpcStreamData && message.stream_id === stream_id) {
                runBlocking {
                    stream.write {
                        it.put(message.u8a)
                    }
                }
            }
//            stream.write (data)
        }
        return stream.toInputStream()
    }
//    when(rawBody.type){
//        TEXT
//                BASE64
//                BINARY
//                STREAM_ID
//                TEXT_STREAM_ID
//                BASE64_STREAM_ID
//                BINARY_STREAM_ID
//    }
}