package info.bagen.dwebbrowser.microService.sys.plugin.haptics

import com.google.gson.reflect.TypeToken
import info.bagen.dwebbrowser.microService.core.BootstrapContext
import info.bagen.dwebbrowser.microService.core.NativeMicroModule
import info.bagen.dwebbrowser.microService.helper.gson
import info.bagen.dwebbrowser.microService.sys.http.Gateway
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.long
import org.http4k.lens.string

import org.http4k.routing.bind
import org.http4k.routing.routes
import java.util.ArrayList

class HapticsNMM : NativeMicroModule("haptics.sys.dweb") {
    private val vibrateManage = VibrateManage()
    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        val query_type = Query.string().optional("style")
        val query_duration = Query.string().required("duration")
        val type_duration = object : TypeToken<ArrayList<Long>>() {}.type
        apiRouting = routes(
            /** 触碰轻质量物体 */
            "/impactLight" bind Method.GET to defineHandler { request ->
                val style = when (query_type(request)) {
                    "MEDIUM" -> HapticsImpactType.MEDIUM
                    "HEAVY" -> HapticsImpactType.HEAVY
                    else -> HapticsImpactType.LIGHT
                }
                vibrateManage.impact(style)
                Response(Status.OK)
            },
            /** 警告分隔的振动通知 */
            "/notification" bind Method.GET to defineHandler { request ->
                val type = when (query_type(request)) {
                    "SUCCESS" -> HapticsNotificationType.SUCCESS
                    "WARNING" -> HapticsNotificationType.WARNING
                    else -> HapticsNotificationType.ERROR
                }
                vibrateManage.notification(type)
                Response(Status.OK)
            },
            /** 单击手势的反馈振动 */
            "/vibrateClick" bind Method.GET to defineHandler { request ->
                vibrateManage.vibrateClick()
                Response(Status.OK)
            },
            /** 禁用手势的反馈振动，与headShak特效一致 */
            "/vibrateDisabled" bind Method.GET to defineHandler { request ->
                vibrateManage.vibrateDisabled()
                Response(Status.OK)
            },
            /** 双击手势的反馈振动 */
            "/vibrateDoubleClick" bind Method.GET to defineHandler { request ->
                vibrateManage.vibrateDoubleClick()
                Response(Status.OK)
            },
            /** 重击手势的反馈振动，比如菜单键/长按/3DTouch */
            "/vibrateHeavyClick" bind Method.GET to defineHandler { request ->
                vibrateManage.vibrateHeavyClick()
                Response(Status.OK)
            },
            /** 滴答 */
            "/vibrateTick" bind Method.GET to defineHandler { request ->
                vibrateManage.vibrateTick()
                Response(Status.OK)
            },
            /** 自定义传递 振动频率 */
            "/customize" bind Method.GET to defineHandler { request ->
                val duration = query_duration(request)
                val durationArray: List<Long> = gson.fromJson(duration, type_duration)
                vibrateManage.vibratePre26(durationArray.toLongArray(), 0)
                Response(Status.OK)
            },
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}