package org.mkdesklayout.project

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

data class NFDataType(val row: Int, val column: Int)

data class NFCacalaterParams(
  val column: Int,
  val screenWidth: Int,
  val hSpace: Int = 10,
  val vSpace: Int = 10
) {

  init {
    require(column >= 1) {"ERRPR: column must be greater than or equal to 1"}
  }

  val itemSize: IntSize by lazy {
    val w = screenWidth - hSpace * (column - 1)
    IntSize(w / column, w / column)
  }
}

data class NFData<T:Any?>(val value: T, val type: NFDataType) {
  val key by lazy { hashCode() }
}

data class NFGeometry(val offset: IntOffset, val size: IntSize)

data class NFLayoutData<T:Any?>(val data: NFData<T>, val geo: NFGeometry){
  val key get() = data.key
}

fun Offset.toInt() = IntOffset(x.toInt(), y.toInt())
