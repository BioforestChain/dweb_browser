package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.helper.IntEnumSerializer
import org.dweb_browser.helper.ProxySerializer
import org.dweb_browser.helper.toBase64
import org.dweb_browser.helper.toBase64Url
import kotlin.random.Random

@Serializable
data class MetaBodyJsonAble(
  val type: MetaBody.IPC_META_BODY_TYPE,
  val senderUid: Int,
  val data: String,
  val streamId: String? = null,
  val receiverUid: Int? = null,
  val metaId: String,
) {
  fun toMetaBody() = MetaBody(type, senderUid, data, streamId, receiverUid, metaId)
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
  val senderUid: Int,
  val data: Any,
  val streamId: String? = null,
  var receiverUid: Int? = null,
  /**
   * 唯一id，指代这个数据的句柄
   *
   * 需要使用这个值对应的数据进行缓存操作
   * 远端可以发送句柄回来，这样可以省去一些数据的回传延迟。
   */
  val metaId: String = randomMetaId()
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
    INLINE_BINARY(INLINE or IPC_DATA_ENCODING.BINARY), ;

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
    private fun randomMetaId() = ByteArray(8).also { Random.nextBytes(it) }.toBase64Url()
    fun fromText(
      senderUid: Int,
      data: String,
      streamId: String? = null,
      receiverUid: Int? = null,
      metaId: String = randomMetaId(),
    ) = MetaBody(
      type = if (streamId == null) IPC_META_BODY_TYPE.INLINE_TEXT else IPC_META_BODY_TYPE.STREAM_WITH_TEXT,
      senderUid = senderUid,
      data = data,
      streamId = streamId,
      receiverUid = receiverUid,
      metaId = metaId
    )

    fun fromBase64(
      senderUid: Int,
      data: String,
      streamId: String? = null,
      receiverUid: Int? = null,
      metaId: String = randomMetaId(),
    ) = MetaBody(
      type = if (streamId == null) IPC_META_BODY_TYPE.INLINE_BASE64 else IPC_META_BODY_TYPE.STREAM_WITH_BASE64,
      senderUid = senderUid,
      data = data,
      streamId = streamId,
      receiverUid = receiverUid,
      metaId = metaId,
    )

    fun fromBinary(
      senderUid: Int,
      data: ByteArray,
      streamId: String? = null,
      receiverUid: Int? = null,
      metaId: String = randomMetaId(),
    ) = MetaBody(
      type = if (streamId == null) IPC_META_BODY_TYPE.INLINE_BINARY else IPC_META_BODY_TYPE.STREAM_WITH_BINARY,
      senderUid = senderUid,
      data = data,
      streamId = streamId,
      receiverUid = receiverUid,
      metaId = metaId,
    )

    fun fromBinary(
      senderIpc: Ipc,
      data: ByteArray,
      streamId: String? = null,
      receiverUid: Int? = null,
      metaId: String = randomMetaId(),
    ) = if (senderIpc.supportBinary) fromBinary(
      senderIpc.uid, data, streamId, receiverUid, metaId
    ) else fromBase64(
      senderIpc.uid, data.toBase64(), streamId, receiverUid, metaId
    )
  }

  val jsonAble by lazy {
    when (type.encoding) {
      IPC_DATA_ENCODING.BINARY -> fromBase64(
        senderUid, (data as ByteArray).toBase64(), streamId, receiverUid, metaId
      )

      else -> this
    }.run {
      MetaBodyJsonAble(
        type,
        senderUid,
        data as String,
        streamId,
        receiverUid,
        metaId,
      )
    }
  }
}


//private fun JsonObject.getElementOrNull(key: String): JsonElement? {
//  if (!has(key)) {
//    return null
//  }
//  val value = get(key)
//  return if (value.isJsonNull) null
//  else value
//}
//
//private fun JsonObject.getStringOrNull(key: String) = getElementOrNull(key)?.asString
//
//private fun JsonObject.getIntOrNull(key: String) = getElementOrNull(key)?.asInt
//
//
