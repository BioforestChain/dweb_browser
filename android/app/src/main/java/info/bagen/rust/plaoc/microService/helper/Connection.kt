package info.bagen.rust.plaoc.microService.helper

import com.daveanthonythomas.moshipack.MoshiPack
import com.google.gson.Gson

// 工具方法
val gson = Gson()
val moshiPack = MoshiPack()

// 构造返回
data class DefaultErrorResponse(
    var statusCode: Number = 502,
    var errorMessage: String = "服务器异常",
    var detailMessage: String? = ""
)


fun rand(start: Int, end: Int): Int {
    require(start <= end) { "Illegal Argument" }
    return (start..end).random()
}







