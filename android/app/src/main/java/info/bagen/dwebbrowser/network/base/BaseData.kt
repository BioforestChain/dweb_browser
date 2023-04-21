package info.bagen.dwebbrowser.network.base

import org.http4k.core.Response
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * 用于获取网络请求数据
 */
data class BaseData<T>(
    val errorCode: Int = 0, // ==0 为成功
    val errorMsg: String = "",
    val data: T?,
    var isSuccess: Boolean = errorCode == 0 && data != null
)

/**
 * 直接返回最终值
 */
suspend inline fun <reified T> Response.bodyData(): T =
    if (this.status.successful) {
        this.body.payload as T // body有做bodyNullable判断，导致会有exception打印，这边做过滤
    } else {
        BaseData(this.status.code, this.status.description, null) as T
    }

fun byteBufferToString(byteBuffer: ByteBuffer) : String {
    val charset = Charset.forName("utf-8")
    val charBuffer = charset.decode(byteBuffer)
    return charBuffer.toString()
}