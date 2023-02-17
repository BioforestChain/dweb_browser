package info.bagen.rust.plaoc.microService.helper

import com.daveanthonythomas.moshipack.MoshiPack
import com.google.gson.Gson
import kotlinx.serialization.*

// 工具方法
val gson = Gson()
val moshiPack = MoshiPack()

// 构造返回
@Serializable
data class DefaultErrorResponse(
    var statusCode: Number = 502,
    var errorMessage: String = "服务器异常",
    var detailMessage: String? = ""
)





