package org.dweb_browser.helper.android

import android.view.View
import androidx.core.view.WindowInsetsCompat
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentSkipListSet

typealias OnApplyWindowInsetsCompatListener = (view: View, insets: WindowInsetsCompat) -> WindowInsetsCompat

val viewOnApplyWindowInsetsCompatListenerMap =
  WeakHashMap<View, ConcurrentSkipListSet<OnApplyWindowInsetsCompatListener>>();

fun View.addOnApplyWindowInsetsCompatListener(listener: OnApplyWindowInsetsCompatListener) {
  viewOnApplyWindowInsetsCompatListenerMap.getOrPut(this) {
    ConcurrentSkipListSet<OnApplyWindowInsetsCompatListener>().also { listeners ->
      setOnApplyWindowInsetsListener { v, insets ->
        var insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets)
        for (cb in listeners) {
          insetsCompat = cb(v, insetsCompat)
        }
        insetsCompat.toWindowInsets() ?: insets
      }
    }
  }.add(listener)
}