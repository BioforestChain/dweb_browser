package info.bagen.dwebbrowser.microService.sys.haptics

import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.string

import org.http4k.routing.bind
import org.http4k.routing.routes

class HapticsNMM : NativeMicroModule("haptics.sys.dweb","haptics") {

    override val categories = mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);

    private val vibrateManage = VibrateManage()
    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        val query_type = Query.string().optional("style")
        val query_duration = Query.string().required("duration")
        apiRouting = routes(
            /** 触碰轻质量物体 */
            "/impactLight" bind Method.GET to defineHandler { request ->
                val style = when (query_type(request)) {
                    "MEDIUM" -> HapticsImpactType.MEDIUM
                    "HEAVY" -> HapticsImpactType.HEAVY
                    else -> HapticsImpactType.LIGHT
                }
                vibrateManage.impact(style)
                ResponseData()
            },
            /** 警告分隔的振动通知 */
            "/notification" bind Method.GET to defineHandler { request ->
                val type = when (query_type(request)) {
                    "SUCCESS" -> HapticsNotificationType.SUCCESS
                    "WARNING" -> HapticsNotificationType.WARNING
                    else -> HapticsNotificationType.ERROR
                }
                vibrateManage.notification(type)
                ResponseData()
            },
            /** 单击手势的反馈振动 */
            "/vibrateClick" bind Method.GET to defineHandler { request ->
                vibrateManage.vibrateClick()
                ResponseData()
            },
            /** 禁用手势的反馈振动，与headShak特效一致 */
            "/vibrateDisabled" bind Method.GET to defineHandler { request ->
                vibrateManage.vibrateDisabled()
                ResponseData()
            },
            /** 双击手势的反馈振动 */
            "/vibrateDoubleClick" bind Method.GET to defineHandler { request ->
                vibrateManage.vibrateDoubleClick()
                ResponseData()
            },
            /** 重击手势的反馈振动，比如菜单键/长按/3DTouch */
            "/vibrateHeavyClick" bind Method.GET to defineHandler { request ->
                vibrateManage.vibrateHeavyClick()
                ResponseData()
            },
            /** 滴答 */
            "/vibrateTick" bind Method.GET to defineHandler { request ->
                vibrateManage.vibrateTick()
                ResponseData()
            },
            /** 自定义传递 振动频率 */
            "/customize" bind Method.GET to defineHandler { request ->
                val duration = query_duration(request)
                try {
                    val array = duration.removeArrayMark().split(",")
                    val longArray = LongArray(array.size) { array[it].toLong() }
                    vibrateManage.vibratePre26(longArray, -1)
                    ResponseData("ok")
                } catch (e: Exception) {
                    Response(Status.EXPECTATION_FAILED).body(e.toString())
                }
            },
        )
    }

    data class ResponseData(val message:String = "ok")

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }

    private fun String.removeArrayMark() = this.replace("[", "").replace("]","")
}