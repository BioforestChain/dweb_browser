package info.bagen.rust.plaoc.microService.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName


// 工具方法
val gson = Gson()

// 构造返回
data class DefaultErrorResponse(
    @SerializedName("statusCode") var statusCode: Number = 502,
    @SerializedName("errorMessage") var errorMessage: String = "服务器异常",
    @SerializedName("detailMessage") var detailMessage: String? = ""
)





