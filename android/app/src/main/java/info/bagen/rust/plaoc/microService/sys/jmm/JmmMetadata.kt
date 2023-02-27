package info.bagen.rust.plaoc.microService.sys.jmm

import info.bagen.rust.plaoc.microService.helper.Mmid
import org.http4k.core.Method
import org.http4k.core.Request

data class JmmMetadata(
    val main_url: String,
    val permissions: List<Mmid> = listOf(),
    var fromRequest: Request = Request(
        Method.GET,
        ""
    ),
    val iconUrl: String = "",
    val title: String = ""
    // (Method.Get, "http://tansocc.com?xxx=d").headers()
)