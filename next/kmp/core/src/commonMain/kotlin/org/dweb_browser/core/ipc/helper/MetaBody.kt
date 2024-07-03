package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.IntEnumSerializer
import org.dweb_browser.helper.ProxySerializer
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.base64String
import org.dweb_browser.helper.base64UrlString
import kotlin.random.Random

@Serializable
data class MetaBodyJsonAble(
  val type: MetaBody.IPC_META_BODY_TYPE,
  val senderUid: UUID,
  val data: String,
  val streamId: String? = null,
  val receiverUid: UUID? = null,
) {
  fun toMetaBody() = MetaBody(type, data, senderUid, receiverUid, streamId)
}


object MetaBodySerializer : ProxySerializer<MetaBody, MetaBodyJsonAble>("MetaBody",
  MetaBodyJsonAble.serializer(),
  { jsonAble },
  { toMetaBody() })

@Serializable(with = MetaBodySerializer::class)
data class MetaBody(
  /**
   * 类型信息，包含了 编码信息 与 形态信息
   * 编码信息是对 data 的解释
   * 形态信息（流、内联）是对 "是否启用 streamId" 的描述（注意，流也可以内联第一帧的数据）
   */
  val type: IPC_META_BODY_TYPE,
  val data: Any,
  /**发送者的uid*/
  val senderPoolId: UUID,
  /**接收者的uid*/
  var receiverPoolId: UUID? = null,
  /**
   * 流ID,跟PureStream(byteReadChannel)绑定
   * 唯一id，指代这个数据的句柄
   *
   * 需要使用这个值对应的数据进行缓存操作
   * 远端可以发送句柄回来，这样可以省去一些数据的回传延迟。
   * */
  val streamId: String? = null,
) {
  @Serializable(with = IPC_META_BODY_TYPE_Serializer::class)
  enum class IPC_META_BODY_TYPE(val type: Int) {
    /** 流 */
    STREAM_ID(0),

    /** 内联数据 */
    INLINE(1),


    /** 文本 json html 等 */
    STREAM_WITH_TEXT(STREAM_ID or IPC_DATA_ENCODING.UTF8),

    /** 使用文本表示的二进制 */
    STREAM_WITH_BASE64(STREAM_ID or IPC_DATA_ENCODING.BASE64),

    /** 二进制 */
    STREAM_WITH_BINARY(STREAM_ID or IPC_DATA_ENCODING.BINARY),

    /** 文本 json html 等 */
    INLINE_TEXT(INLINE or IPC_DATA_ENCODING.UTF8),

    /** 使用文本表示的二进制 */
    INLINE_BASE64(INLINE or IPC_DATA_ENCODING.BASE64),

    /** 二进制 */
    INLINE_BINARY(INLINE or IPC_DATA_ENCODING.BINARY),
    ;

    val encoding by lazy {
      val encoding = type and 0b11111110;
      IPC_DATA_ENCODING.ALL_VALUES[encoding]
    }
    val isInline by lazy {
      type and 1 == 1
    }
    val isStream by lazy {
      type and 1 == 0
    }

    companion object {
      val ALL_VALUES = entries.associateBy { it.type }
    }

    private inline infix fun or(encoding: IPC_DATA_ENCODING) = type or encoding.encoding
  }

  @Suppress("ClassName")
  object IPC_META_BODY_TYPE_Serializer : IntEnumSerializer<IPC_META_BODY_TYPE>(
    "IPC_META_BODY_TYPE",
    IPC_META_BODY_TYPE.ALL_VALUES,
    { type })

  companion object {
    private fun randomMetaId() = ByteArray(8).also { Random.nextBytes(it) }.base64UrlString
    fun fromText(
      data: String,
      senderPoolId: UUID,
      streamId: String? = null,
      receiverPoolId: UUID? = null,
    ) = MetaBody(
      type = if (streamId == null) IPC_META_BODY_TYPE.INLINE_TEXT else IPC_META_BODY_TYPE.STREAM_WITH_TEXT,
      senderPoolId = senderPoolId,
      data = data,
      streamId = streamId,
      receiverPoolId = receiverPoolId
    )

    fun fromBase64(
      data: String,
      senderPoolId: UUID,
      streamId: String? = null,
      receiverPoolId: UUID? = null,
    ) = MetaBody(
      type = if (streamId == null) IPC_META_BODY_TYPE.INLINE_BASE64 else IPC_META_BODY_TYPE.STREAM_WITH_BASE64,
      senderPoolId = senderPoolId,
      data = data,
      streamId = streamId,
      receiverPoolId = receiverPoolId
    )

    fun fromBinary(
      data: ByteArray,
      senderPoolId: UUID,
      receiverPoolId: UUID? = null,
      streamId: String? = null,
    ) = MetaBody(
      type = if (streamId == null) IPC_META_BODY_TYPE.INLINE_BINARY else IPC_META_BODY_TYPE.STREAM_WITH_BINARY,
      senderPoolId = senderPoolId,
      data = data,
      streamId = streamId,
      receiverPoolId = receiverPoolId
    )
  }

  val jsonAble by lazy {
    when (type.encoding) {
      IPC_DATA_ENCODING.BINARY -> fromBase64(
        (data as ByteArray).base64String, senderPoolId, streamId, receiverPoolId
      )

      else -> this
    }.run {
      MetaBodyJsonAble(
        type,
        senderPoolId,
        data as String,
        streamId,
        receiverPoolId
      )
    }
  }
}