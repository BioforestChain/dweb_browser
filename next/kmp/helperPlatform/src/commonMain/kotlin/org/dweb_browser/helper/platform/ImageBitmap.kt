package org.dweb_browser.helper.platform

import androidx.compose.ui.graphics.ImageBitmap
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * ## 官方 ByteArray 转化为 ImageBitmap
 * ### 注意: 当前支持的格式 JPEG, PNG, BMP, or WEBP
 * @see: https://www.jetbrains.com/help/kotlin-multiplatform-dev/whats-new-compose-170.html#functions-for-converting-byte-arrays-into-imagebitmap-or-imagevector
 * */
@OptIn(ExperimentalResourceApi::class)
fun ByteArray.kmpDecodeToImageBitmap() = decodeToImageBitmap()

expect fun ByteArray.toImageBitmap(): ImageBitmap?

expect fun ImageBitmap.toByteArray(): ByteArray?