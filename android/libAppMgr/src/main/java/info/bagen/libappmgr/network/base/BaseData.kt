package info.bagen.libappmgr.network.base

import io.ktor.client.call.*
import io.ktor.client.statement.*

/**
 * 用于获取网络请求数据
 */
data class BaseData<T>(
    val errorCode: Int, // ==0 为成功
    val errorMsg: String,
    val data: T?,
    var isSuccess: Boolean = errorCode == 0 && data != null
)

/**
 * 直接返回最终值
 */
suspend inline fun <reified T> HttpResponse.bodyData(): T =
    if (this.status.value == 200) {
        this.body() // body有做bodyNullable判断，导致会有exception打印，这边做过滤
    } else {
        BaseData(this.status.value, this.status.description, null) as T
    }

