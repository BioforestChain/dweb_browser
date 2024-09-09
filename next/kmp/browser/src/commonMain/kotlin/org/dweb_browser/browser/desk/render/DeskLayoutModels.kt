package org.dweb_browser.browser.desk.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.serialization.Serializable


data class NFCacalaterParams(
  val column: Int,
  val screenWidth: Int,
  val hSpace: Int = 10,
  val vSpace: Int = 10,
  val aspectRatio: Pair<Int, Int> = Pair(1, 1) // 宽高比，默认为1 : 1
) {

  val itemSize: IntSize by lazy {
    if (screenWidth <= 0) {
      return@lazy IntSize.Zero
    }
    val w = screenWidth - hSpace * (column - 1).coerceAtLeast(0)
    IntSize(w / column, w / column / aspectRatio.first * aspectRatio.second)
  }
}

//棋盘上OffSet
typealias NFSpaceCoordinateOffSet = IntOffset

//棋盘上Size
typealias NFSpaceCoordinateSize = IntSize

//棋盘上Geometry
typealias NFSpaceCoordinateLayout = NFGeometry

//实际layout的Geometry
@Serializable
data class NFGeometry(val x: Int, val y: Int, val width: Int, val height: Int) {
  val offset: IntOffset
    get() = IntOffset(x, y)

  val size: IntSize
    get() = IntSize(width, height)

  companion object {
    fun from(offset: IntOffset, size: IntSize): NFGeometry {
      return NFGeometry(offset.x, offset.y, size.width, size.height)
    }
  }
}

typealias NFDataType = NFSpaceCoordinateSize

//布局模型
data class NFLayoutData<T>(val data: T, val sCGeo: NFSpaceCoordinateLayout, val geo: NFGeometry) {
  val key get() = data.hashCode()
}


fun Offset.toInt() = IntOffset(x.toInt(), y.toInt())
