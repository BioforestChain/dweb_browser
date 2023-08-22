package org.dweb_browser.helper.android

import android.view.View
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentSkipListSet

typealias OnApplyWindowInsetsCompatListener = (view: View, insets: WindowInsetsCompat) -> WindowInsetsCompat

val viewOnApplyWindowInsetsCompatListenerMap =
  WeakHashMap<View, MutableSet<OnApplyWindowInsetsCompatListener>>();

fun View.addOnApplyWindowInsetsCompatListener(listener: OnApplyWindowInsetsCompatListener) {
  viewOnApplyWindowInsetsCompatListenerMap.getOrPut(this) {
    mutableSetOf<OnApplyWindowInsetsCompatListener>().also { listeners ->
      setOnApplyWindowInsetsListener { v, insets ->
        var insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets)
        synchronized(listeners) {
          for (cb in listeners) {
            insetsCompat = cb(v, insetsCompat)
          }
        }
        insetsCompat.toWindowInsets() ?: insets
      }
    }
  }.also { synchronized(it) { it.add(listener) } }
}
