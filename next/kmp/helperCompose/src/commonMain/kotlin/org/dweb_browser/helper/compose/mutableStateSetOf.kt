package org.dweb_browser.helper.compose

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.snapshots.StateFactoryMarker


class SnapshotStateSet<T>(private val map: SnapshotStateMap<T, Unit>) : MutableSet<T> by map.keys {
  override fun add(element: T): Boolean = map.put(element, Unit) == null
  override fun addAll(elements: Collection<T>): Boolean {
    var added = false
    for (ele in elements) {
      added = map.put(ele, Unit) == null || added
    }
    return added
  }
}

@StateFactoryMarker
fun <T> mutableStateSetOf() = SnapshotStateSet<T>(mutableStateMapOf())