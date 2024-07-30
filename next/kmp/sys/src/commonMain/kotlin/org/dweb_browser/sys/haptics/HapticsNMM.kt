package org.dweb_browser.sys.haptics

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
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
        /** 触碰有质量物体 */
        "/impact" bind PureMethod.GET by defineEmptyResponse {
          val style = request.queryOrNull("style")?.let { HapticsImpactType.ALL[it] }
            ?: HapticsImpactType.LIGHT
          vibrateManage.impact(style)
        },
        /** 警告分隔的振动通知 */
        "/notification" bind PureMethod.GET by defineEmptyResponse {
          val style = request.queryOrNull("style")?.let { HapticsNotificationType.ALL[it] }
            ?: HapticsNotificationType.ERROR
          vibrateManage.notification(style)
        },
        /** 单击手势的反馈振动 */
        "/click" bind PureMethod.GET by defineEmptyResponse {
          vibrateManage.vibrateClick()
        },
        /** 禁用手势的反馈振动，与headShak特效一致 */
        "/disabled" bind PureMethod.GET by defineEmptyResponse {
          vibrateManage.vibrateDisabled()
        },
        /** 双击手势的反馈振动 */
        "/doubleClick" bind PureMethod.GET by defineEmptyResponse {
          vibrateManage.vibrateDoubleClick()
        },
        /** 重击手势的反馈振动，比如菜单键/长按/3DTouch */
        "/heavyClick" bind PureMethod.GET by defineEmptyResponse {
          vibrateManage.vibrateHeavyClick()
        },
        /** 滴答 */
        "/tick" bind PureMethod.GET by defineEmptyResponse {
          vibrateManage.vibrateTick()
        },
        /** 自定义传递 振动频率 */
        "/customize" bind PureMethod.GET by defineEmptyResponse {
          val duration = request.query("duration")
          val array = duration.removeArrayMark().split(",")
          val longArray = LongArray(array.size) { array[it].toLong() }
          vibrateManage.vibratePre26(longArray, -1)
        },
        /**
         * 以下 接口设计错误，将要被废弃
         */
        /** 触碰轻质量物体 */
        "/impactLight" bind PureMethod.GET by defineEmptyResponse {
          val style = request.queryOrNull("style")?.let { HapticsImpactType.ALL[it] }
            ?: HapticsImpactType.LIGHT
          vibrateManage.impact(style)
        },
        /** 单击手势的反馈振动 */
        "/vibrateClick" bind PureMethod.GET by defineEmptyResponse {
          vibrateManage.vibrateClick()
        },
        /** 禁用手势的反馈振动，与headShak特效一致 */
        "/vibrateDisabled" bind PureMethod.GET by defineEmptyResponse {
          vibrateManage.vibrateDisabled()
        },
        /** 双击手势的反馈振动 */
        "/vibrateDoubleClick" bind PureMethod.GET by defineEmptyResponse {
          vibrateManage.vibrateDoubleClick()
        },
        /** 重击手势的反馈振动，比如菜单键/长按/3DTouch */
        "/vibrateHeavyClick" bind PureMethod.GET by defineEmptyResponse {
          vibrateManage.vibrateHeavyClick()
        },
        /** 滴答 */
        "/vibrateTick" bind PureMethod.GET by defineEmptyResponse {
          vibrateManage.vibrateTick()
        },
      ).cors()
    }

    override suspend fun _shutdown() {

    }

  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = HapticsRuntime(bootstrapContext)
  private fun String.removeArrayMark() = this.replace("[", "").replace("]", "")
}