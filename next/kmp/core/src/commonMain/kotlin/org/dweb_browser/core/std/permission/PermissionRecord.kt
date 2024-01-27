package org.dweb_browser.core.std.permission

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.StringEnumSerializer
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.randomUUID

val debugPermission = Debugger("permission")
typealias PERMISSION_ID = String

/**
 * 授权记录
 *
 * 这里默认提供常见的基于时间授权过期
 */
@Serializable
data class AuthorizationRecord(
  /**
   * 权限唯一标识
   */
  val pid: PERMISSION_ID,
  /**
   * 权限申请者
   */
  @SerialName("mmid") val applicantMmid: MMID,
  /**
   * 记录的过期时间
   *
   * granted 状态下
   * 如果是负数，意味着永久授权（一般用于内部模块之前的授权）
   * 默认有效期是一个礼拜（一般用于jmm应用的授权）
   *
   * denied 状态下
   * 如果是负数，意味着永久拒绝授权
   * 默认情况是当下时间，就是可以重新发起
   */
  val expirationTime: Long,
  /**
   * 授权状态
   */
  val status: AuthorizationStatus,
) {

  /**
   * 权限提供者
   */
  val providerMmid by lazy { pid.split('/', limit = 2)[0] }
  val safeStatus
    get() = if (expirationTime < 0L || expirationTime > datetimeNow()) status else
    // 如果已经过期，返回 unknown
      AuthorizationStatus.UNKNOWN

  companion object{

    fun generateAuthorizationRecord(pid: PERMISSION_ID, applicantMmid: MMID, granted: Boolean?) =
      when (granted) {
        true -> AuthorizationRecord(
          pid = pid,
          applicantMmid = applicantMmid,
          expirationTime = datetimeNow() + 7 * 24 * 60 * 60 * 1000,
          status = AuthorizationStatus.GRANTED
        )

        false -> AuthorizationRecord(
          pid = pid,
          applicantMmid = applicantMmid,
          expirationTime = datetimeNow() + 1000,
          status = AuthorizationStatus.DENIED
        )

        null -> AuthorizationRecord(
          pid = pid,
          applicantMmid = applicantMmid,
          expirationTime = Long.MAX_VALUE,
          status = AuthorizationStatus.UNKNOWN
        )
      }

  }
}

object AuthorizationStatusSerializer : StringEnumSerializer<AuthorizationStatus>(
  "AuthorizationStatus",
  AuthorizationStatus.ALL_VALUES,
  { status })

@Serializable(AuthorizationStatusSerializer::class)
enum class AuthorizationStatus(val status: String, val allow: Boolean) {
  UNKNOWN("unknown", false),//
  GRANTED("granted", false),//
  DENIED("denied", true),//
  ;

  companion object {
    val ALL_VALUES = entries.associateBy { it.status }
  }
}
