package org.dweb_browser.browser.desk.render

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize


class NFCaculater() {
  companion object {
    fun sizeReq(toPlaceType: NFDataType, params: NFCacalaterParams): IntSize {
      return IntSize(
        params.itemSize.width * toPlaceType.width + params.hSpace * (toPlaceType.width - 1).coerceAtLeast(
          0
        ),
        params.itemSize.height * toPlaceType.height + params.vSpace * (toPlaceType.height - 1).coerceAtLeast(
          0
        ),
      )
    }

    fun <T> searchAreas(
      offset: IntOffset,
      layout: NFLayoutData<T>,
      params: NFCacalaterParams,
    ): NFLayoutData<T> {

      val triggler = IntOffset(
        params.itemSize.width / 2,
        params.itemSize.height / 2
      )

      var indexX = (offset.x + triggler.x) / (params.itemSize.width + params.hSpace)
      var indexY = (offset.y + triggler.y) / (params.itemSize.height + params.vSpace)

      indexX = indexX.coerceIn(0 until params.column)
      indexY = indexY.coerceAtLeast(0)

      return if (indexX == layout.sCGeo.x && indexY == layout.sCGeo.y) {
        layout
      } else {
        val geo = layout.geo.copy(
          x = indexX * (params.itemSize.width + params.hSpace),
          y = indexY * (params.itemSize.height + params.vSpace)
        )
        val scGeo = layout.sCGeo.copy(indexX, indexY)
        layout.copy(sCGeo = scGeo, geo = geo)
      }
    }

    fun <T> getLayout(
      value: T,
      spaceCoordinateLayout: NFSpaceCoordinateLayout,
      params: NFCacalaterParams
    ): NFLayoutData<T> {

      val offX = spaceCoordinateLayout.offset.x
      val offY = spaceCoordinateLayout.offset.y

      val offset = IntOffset(
        x = offX * (params.itemSize.width + params.hSpace),
        y = offY * (params.itemSize.height + params.vSpace)
      )

      val sizeW = spaceCoordinateLayout.size.width
      val sizeH = spaceCoordinateLayout.size.height

      val size = IntSize(
        params.itemSize.width * sizeW + params.hSpace * (sizeW - 1).coerceAtLeast(0),
        params.itemSize.height * sizeH + params.vSpace * (sizeH - 1).coerceAtLeast(0),
      )

      return NFLayoutData(value, spaceCoordinateLayout, NFGeometry.from(offset, size))
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
      offset: NFSpaceCoordinateOffSet,
      size: NFSpaceCoordinateSize,
      inBoard: MutableList<BooleanArray>,
      column: Int
    ): Boolean {
//    println("FUUU checkIsUseful ${position.first}-${position.second}")
      val toFillSet = mutableSetOf<Pair<Int, Int>>()
      var r = offset.y

      while (r < offset.y + size.height) {

        while (r >= inBoard.size) {
          spaceBoardExpand(inBoard, column)
        }

        val row = inBoard[r]
        var c = offset.x

        if (c + size.width > row.size) {
          return false
        }

        var count = 0
        while (c < row.size && count < size.width) {
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

    fun spaceBoardExpand(inBoard: MutableList<BooleanArray>, column: Int) {
      inBoard.add(BooleanArray(column) { true })
    }

    fun <T> layout(
      layouts: List<NFLayoutData<T>>,
      blockLayouts: List<NFLayoutData<T>>,
      params: NFCacalaterParams,
      refresh: Boolean
    ): List<NFLayoutData<T>> {
      val spaceBoard = MutableList(8) { BooleanArray(params.column) { true } }

      blockLayouts.forEach {
        val scGeo = it.sCGeo
        while (scGeo.y >= spaceBoard.count()) {
          spaceBoardExpand(spaceBoard, params.column)
        }
        for (y in scGeo.y until scGeo.y + scGeo.height) {
          for (x in scGeo.x until scGeo.x + scGeo.width) {
            spaceBoard[y][x] = false
          }
        }
      }

//      logBoard(spaceBoard, "SYNC BLOCKS")

      //从指定位置开始遍历所有指类型的子board，找出第一个可用位置。
      fun findNextAvailablePosition(
        startOffSet: NFSpaceCoordinateOffSet,
        size: NFSpaceCoordinateSize
      ): Pair<IntOffset, NFSpaceCoordinateOffSet> {
        var r = startOffSet.y
        while (true) {
          var c = if (r == startOffSet.y) startOffSet.x else 0
          while (c < params.column) {
            val isUseFull = checkIsUseful(IntOffset(c, r), size, spaceBoard, params.column)
            if (isUseFull) {
              val offset = IntOffset(
                c * (params.itemSize.width + params.hSpace),
                r * (params.itemSize.height + params.vSpace)
              )
              return Pair(offset, IntOffset(c, r))
            }
            c++
          }
          r++
        }
      }

      fun resort(): List<NFLayoutData<T>> {
        return layouts.sortedBy {
          it.sCGeo.x + it.sCGeo.y * params.column
        }
      }

      val result = mutableListOf<NFLayoutData<T>>()
      var start = NFSpaceCoordinateOffSet.Zero
      val resortLayouts = resort()
      for (index in 0 until resortLayouts.count()) {
        val layout = resortLayouts[index]
        if (!refresh) {
          val isCanInsert =
            checkIsUseful(layout.sCGeo.offset, layout.sCGeo.size, spaceBoard, params.column)
          if (isCanInsert) {
            result.add(layout)
            continue
          }
        }

        val r = findNextAvailablePosition(start, layout.sCGeo.size)
        val s = sizeReq(layout.sCGeo.size, params)
        result.add(
          NFLayoutData(
            layout.data,
            NFGeometry.from(r.second, layout.sCGeo.size),
            NFGeometry.from(r.first, s)
          )
        )
        start = r.second
      }
      return result
    }
  }
}