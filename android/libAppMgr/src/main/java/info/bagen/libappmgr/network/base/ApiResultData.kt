package info.bagen.libappmgr.network.base

import io.ktor.client.call.*
import io.ktor.client.statement.*
import java.io.File

/*const val BASE_URL = "172.30.93.165"
const val BASE_PORT = 8080
const val BASE_URL_PATH = "http://$BASE_URL:$BASE_PORT/"*/
const val BASE_URL = "shop.plaoc.com"
const val BASE_PORT = 80
const val BASE_URL_PATH = "https://shop.plaoc.com/"
/*const val BASE_URL = "linge.plaoc.com"
const val BASE_PORT = 80
const val BASE_URL_PATH = "http://linge.plaoc.com/"*/


/*typealias _ERROR = suspend (Throwable) -> Unit
typealias _PROGRESS = suspend (downloadedSize: Long, length: Long, progress: Float) -> Unit
typealias _SUCCESS<T> = suspend (result: T) -> Unit*/

/*sealed class ApiResult<T>() {
    data class BizSuccess<T>(val errorCode: Int, val errorMsg: String, val data: T) : ApiResult<T>()
    data class BizError(val errorCode: Int, val errorMsg: String) : ApiResult<Nothing>()
    data class OtherError(val throwable: Throwable) : ApiResult<Nothing>()
}*/

interface IApiResult<T> {
    fun onPrepare() {}// 网络请求前
    fun onSuccess(errorCode: Int, errorMsg: String, data: T) {} // 网络请求成功，且返回数据
    fun onError(errorCode: Int, errorMsg: String, exception: Throwable? = null) {}
    fun downloadProgress(current: Long, total: Long, progress: Float) {} // 下载进度
    fun downloadSuccess(file: File) {} // 下载完成
}

//data class BaseData<T>(val errorCode: Int, val errorMsg: String, val data: T?)

class ApiResultData<out T> constructor(val value: Any?) {
    val isSuccess: Boolean get() = value !is Failure && value !is Progress && value !is Prepare
    val isFailure: Boolean get() = value is Failure
    val isLoading: Boolean get() = value is Progress
    val isPrepare: Boolean get() = value is Prepare

    fun exceptionOrNull(): Throwable? = when (value) {
        is Failure -> value.exception
        else -> null
    }

    companion object {
        fun <T> success(value: T): ApiResultData<T> = ApiResultData(value)

        fun <T> failure(exception: Throwable): ApiResultData<T> =
            ApiResultData(createFailure(exception))

        fun <T> prepare(exception: Throwable? = null): ApiResultData<T> =
            ApiResultData(createPrepare(exception))

        fun <T> progress(
            currentLength: Long = 0L, length: Long = 0L, progress: Float = 0f
        ): ApiResultData<T> = ApiResultData(createLoading(currentLength, length, progress))
    }

    data class Failure(val exception: Throwable)

    data class Progress(val currentLength: Long, val length: Long, val progress: Float)

    data class Prepare(val exception: Throwable?)

}

private fun createPrepare(exception: Throwable?): ApiResultData.Prepare =
    ApiResultData.Prepare(exception)

private fun createFailure(exception: Throwable): ApiResultData.Failure =
    ApiResultData.Failure(exception)


private fun createLoading(currentLength: Long, length: Long, progress: Float) =
    ApiResultData.Progress(currentLength, length, progress)


inline fun <R, T> ApiResultData<T>.fold(
    onSuccess: (value: T) -> R,
    onLoading: (loading: ApiResultData.Progress) -> R,
    onFailure: (exception: Throwable?) -> R,
    onPrepare: (exception: Throwable?) -> R
): R {
    return when {
        isFailure -> {
            onFailure(exceptionOrNull())
        }
        isLoading -> {
            onLoading(value as ApiResultData.Progress)
        }
        isPrepare -> {
            onPrepare(exceptionOrNull())
        }
        else -> {
            onSuccess(value as T)
        }
    }
}

inline fun <R> runCatching(block: () -> R): ApiResultData<R> {
    return try {
        ApiResultData.success(block())
    } catch (e: Throwable) {
        ApiResultData.failure(e)
    }
}

suspend inline fun <reified T> HttpResponse.checkAndBody(): T = if (this.status.value == 200) {
    this.body() // body有做bodyNullable判断，导致会有exception打印，这边做过滤
} else {
    BaseData(this.status.value, this.status.description, null) as T
}
