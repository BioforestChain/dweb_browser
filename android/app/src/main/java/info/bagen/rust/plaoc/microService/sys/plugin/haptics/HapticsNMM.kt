package info.bagen.rust.plaoc.microService.sys.plugin.haptics

import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.sys.plugin.clipboard.ClipboardNMM
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.long
import org.http4k.lens.string

import org.http4k.routing.bind
import org.http4k.routing.routes

class HapticsNMM: NativeMicroModule("haptics.sys.dweb") {
    private val vibrateManage = VibrateManage()
    override suspend fun _bootstrap() {
        apiRouting = routes(
            /** 触碰轻质量物体 */
            "/impactLight" bind Method.GET to defineHandler { request ->
                println("Clipboard#apiRouting read===>$mmid  ${request.uri.path} ")
                val style = when (Query.string().optional("type")(request)) {
                    "MEDIUM" -> HapticsImpactType.MEDIUM
                    "HEAVY" -> HapticsImpactType.HEAVY
                    else -> HapticsImpactType.LIGHT
                }
                vibrateManage.impact(style)
                Response(Status.OK)
            },
            /** 警告分隔的振动通知 */
            "/notification" bind Method.GET to defineHandler { request ->
                println("Clipboard#apiRouting read===>$mmid  ${request.uri.path} ")
                val type = when (Query.string().required("type")(request)) {
                    "SUCCESS" -> HapticsNotificationType.SUCCESS
                    "WARNING" -> HapticsNotificationType.WARNING
                    "ERROR" -> HapticsNotificationType.ERROR
                    else -> null
                }
                if (type == null) {
                    Response(Status.UNSATISFIABLE_PARAMETERS).body("HapticsNotification type param error null")
                } else {
                    vibrateManage.notification(type)
                    Response(Status.OK)
                }
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
                println("Clipboard#apiRouting vibrateTick===>$mmid  ${request.uri.path} ")
                vibrateManage.vibrateTick()
                Response(Status.OK)
            },
            /** 自定义传递 振动频率 */
            "/customize" bind Method.GET to defineHandler { request ->
                val type = Query.long().required("type")(request)
                println("Clipboard#apiRouting customize===>$mmid  ${request.uri.path} ")
                vibrateManage.vibrate(type)
                Response(Status.OK)
            },
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}