package org.mkdesklayout.project

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize


class NFCaculater() {
  companion object {
    fun sizeReq(toPlaceType: NFDataType, params: NFCacalaterParams): IntSize {
      return IntSize(
        params.itemSize.width * toPlaceType.column + params.hSpace * (toPlaceType.column-1),
        params.itemSize.height * toPlaceType.row + params.vSpace* (toPlaceType.row-1),
      )
    }

    fun searchAreas1(
      offset: IntOffset,
      size: IntSize,
      params: NFCacalaterParams,
    ): NFGeometry {

      val triggler = IntOffset(
        params.itemSize.width / 2,
        params.itemSize.height / 2
      )

      val indexX = (offset.x + triggler.x) / (params.itemSize.width + params.hSpace)
      val indexY = (offset.y + triggler.y) / (params.itemSize.height + params.vSpace)

      return NFGeometry(
        IntOffset(
          indexX * (params.itemSize.width + params.hSpace),
          indexY * (params.itemSize.height + params.vSpace)
        ),
        size
      )
    }

    //
    fun getSapceCoordinates(geo: NFGeometry, params: NFCacalaterParams): List<Pair<Int, Int>> {
      var c = geo.offset.x / (params.itemSize.width + params.hSpace)
      var r = geo.offset.y / (params.itemSize.height + params.vSpace)

      //防止溢出边界
      c = c.coerceIn(0, params.column - 1)
      r = r.coerceAtLeast(0)

      val w = geo.size.width / (params.itemSize.width + params.hSpace) + 1
      val h = geo.size.height / (params.itemSize.height + params.vSpace) + 1

      val result = mutableListOf<Pair<Int, Int>>()

      for (sr in r until r + h) {
        for (sc in c until c + w) {
          result.add(Pair(sr, sc))
        }
      }

      return result.filter {
        it.second < params.column //防止溢出边界
      }
    }

    fun getSpaceCoordinateRequest(geo: NFGeometry, params: NFCacalaterParams): Pair<Pair<Int, Int>, NFDataType> {
      var c = geo.offset.x / (params.itemSize.width + params.hSpace)
      var r = geo.offset.y / (params.itemSize.height + params.vSpace)

      //防止溢出边界
      c = c.coerceIn(0, params.column - 1)
      r = r.coerceAtLeast(0)

      val w = geo.size.width / (params.itemSize.width + params.hSpace) + 1
      val h = geo.size.height / (params.itemSize.height + params.vSpace) + 1

      return Pair(Pair(r, c), NFDataType(h, w))
    }


    fun logBoard(board: MutableList<BooleanArray>, tag: String = "") {
      println("FUUU logBoard: <<<<<< $tag >>>>>>>")
      board.forEachIndexed { index, v ->
        var info = "$index: "
        info += v.joinToString(",") { if (it) "O" else "X" }
        println("FUUU logBoard:" + info)
      }
      println("FUUU logBoard: >>>>>>> $tag <<<<<<<")
    }

    // 检查是否指点位置，指定的类似的子board是否可用。
    fun checkIsUseful(
      position: Pair<Int, Int>,
      toPlaceType: NFDataType,
      inBoard: MutableList<BooleanArray>,
      column: Int
    ): Boolean {
//    println("FUUU checkIsUseful ${position.first}-${position.second}")
      val req = getSpaceRequest(toPlaceType)
      val toFillSet = mutableSetOf<Pair<Int, Int>>()
      var r = position.first

      while (r < position.first + req.first) {

        while (r >= inBoard.size) {
          spaceBoardExpand(inBoard, column)
        }

        val row = inBoard[r]
        var c = position.second

        if (c + req.second > row.size) {
          return false
        }

        var count = 0
        while (c < row.size && count < req.second) {
          count++
          if (!row[c]) {
            return false
          } else {
            toFillSet.add(Pair(r, c))
          }
          c++
        }
        r++
      }

      toFillSet.forEach {
        inBoard[it.first][it.second] = false
      }

      return true
    }

    fun getSpaceRequest(toPlaceType: NFDataType): Pair<Int, Int> {
      return toPlaceType.row to toPlaceType.column
    }

    fun spaceBoardExpand(inBoard: MutableList<BooleanArray>, column: Int) {
      inBoard.add(BooleanArray(column) { true })
    }

    fun <T> layout(
      layouts: List<NFLayoutData<T>>,
      blockAreas: List<NFGeometry>,
      params: NFCacalaterParams,
      refresh: Boolean
    ): List<NFLayoutData<T>> {
      println("FUUU layout, refresh: $refresh, count: ${layouts.size} ,${layouts.joinToString { "${it.data.value}, " }}")
      println("FUUU layout block, ${blockAreas.joinToString { "${it}, " }}")
      val spaceBoard = MutableList(8) { BooleanArray(params.column) { true } }

      blockAreas.flatMap {
        getSapceCoordinates(it, params)
      }.forEach {
        while (it.first >= spaceBoard.count()) {
          spaceBoardExpand(spaceBoard, params.column)
        }
        spaceBoard[it.first][it.second] = false
      }

      logBoard(spaceBoard, "SYNC BLOCKS")

      //从指定位置开始遍历所有指类型的子board，找出第一个可用位置。
      fun findNextAvailablePosition(
        start: Pair<Int, Int>,
        toPlaceType: NFDataType
      ): Pair<IntOffset, Pair<Int, Int>> {
        var r = start.first
        while (true) {
          var c = if (r == start.first) start.second else 0
          while (c < params.column) {
            val isUseFull = checkIsUseful(Pair(r, c), toPlaceType, spaceBoard, params.column)
            if (isUseFull) {
              val offset = IntOffset(
                c * (params.itemSize.width + params.hSpace),
                r * (params.itemSize.height + params.vSpace)
              )

              val next = Pair(r, c)
              return Pair(offset, next)
            }
            c++
          }
          r++
        }
      }

      var result = mutableListOf<NFLayoutData<T>>()
      var start = Pair(0, 0)
      for (index in 0 until layouts.count()) {
        val layout = layouts[index]

        if (!refresh) {
          val toPlaceReq = getSpaceCoordinateRequest(layout.geo, params)
          val isCanUse = checkIsUseful(toPlaceReq.first, toPlaceReq.second, spaceBoard, params.column)
          if (isCanUse) {
            result.add(NFLayoutData(layout.data, layout.geo.copy(size = sizeReq(layout.data.type, params))))
            continue
          }
        }

        var r = findNextAvailablePosition(start, layout.data.type)
        val s = sizeReq(layout.data.type, params)
        result.add(NFLayoutData(layout.data, NFGeometry(r.first, s)))
        start = r.second
      }
      return result
    }
  }
}