package info.bagen.rust.plaoc.microService.helper

import com.daveanthonythomas.moshipack.MoshiPack
import com.google.gson.Gson

// 工具方法
val gson = Gson()
val moshiPack = MoshiPack()


fun rand(start: Int, end: Int): Int {
    require(start <= end) { "Illegal Argument" }
    return (start..end).random()
}







