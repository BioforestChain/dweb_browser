package org.dweb_browser.sys.haptics

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureMethod

class HapticsNMM : NativeMicroModule("haptics.sys.dweb", "haptics") {

  init {
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);
  }


  @Serializable
  data class ResponseData(val message: String = "ok")

  inner class HapticsRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {

    private val vibrateManage = VibrateManage()
    override suspend fun _bootstrap() {
//    val query_type = Query.string().optional("style")
//    val query_duration = Query.string().required("duration")
      routes(
        /** 触碰轻质量物体 */
        "/impactLight" bind PureMethod.GET by defineJsonResponse {
          val style = when (request.queryOrNull("style")) {
            "MEDIUM" -> HapticsImpactType.MEDIUM
            "HEAVY" -> HapticsImpactType.HEAVY
            else -> HapticsImpactType.LIGHT
          }
          vibrateManage.impact(style)
          ResponseData().toJsonElement()
        },
        /** 警告分隔的振动通知 */
        "/notification" bind PureMethod.GET by defineJsonResponse {
          val type = when (request.queryOrNull("style")) {
            "SUCCESS" -> HapticsNotificationType.SUCCESS
            "WARNING" -> HapticsNotificationType.WARNING
            else -> HapticsNotificationType.ERROR
          }
          vibrateManage.notification(type)
          ResponseData().toJsonElement()
        },
        /** 单击手势的反馈振动 */
        "/vibrateClick" bind PureMethod.GET by defineJsonResponse {
          vibrateManage.vibrateClick()
          ResponseData().toJsonElement()
        },
        /** 禁用手势的反馈振动，与headShak特效一致 */
        "/vibrateDisabled" bind PureMethod.GET by defineJsonResponse {
          vibrateManage.vibrateDisabled()
          ResponseData().toJsonElement()
        },
        /** 双击手势的反馈振动 */
        "/vibrateDoubleClick" bind PureMethod.GET by defineJsonResponse {
          vibrateManage.vibrateDoubleClick()
          ResponseData().toJsonElement()
        },
        /** 重击手势的反馈振动，比如菜单键/长按/3DTouch */
        "/vibrateHeavyClick" bind PureMethod.GET by defineJsonResponse {
          vibrateManage.vibrateHeavyClick()
          ResponseData().toJsonElement()
        },
        /** 滴答 */
        "/vibrateTick" bind PureMethod.GET by defineJsonResponse {
          vibrateManage.vibrateTick()
          ResponseData().toJsonElement()
        },
        /** 自定义传递 振动频率 */
        "/customize" bind PureMethod.GET by defineJsonResponse {
          val duration = request.query("duration")
          try {
            val array = duration.removeArrayMark().split(",")
            val longArray = LongArray(array.size) { array[it].toLong() }
            vibrateManage.vibratePre26(longArray, -1)
            ResponseData("ok").toJsonElement()
          } catch (e: Exception) {
            ResponseData(e.toString()).toJsonElement()
          }
        },
      ).cors()
    }

    override suspend fun _shutdown() {

    }

  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = HapticsRuntime(bootstrapContext)
  private fun String.removeArrayMark() = this.replace("[", "").replace("]", "")
}