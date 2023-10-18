package org.dweb_browser.helper.android

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import java.util.WeakHashMap

typealias OnApplyWindowInsetsCompatListener = (view: View, insets: WindowInsetsCompat) -> WindowInsetsCompat

val viewOnApplyWindowInsetsCompatListenerMap =
  WeakHashMap<View, MutableSet<OnApplyWindowInsetsCompatListener>>();

fun View.addOnApplyWindowInsetsCompatListener(listener: OnApplyWindowInsetsCompatListener) =
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
  }.also { synchronized(it) { it.add(listener) } }.let {
    {
      synchronized(it) { it.remove(listener) }
    }
  }


typealias WindowInsetsAnimationListener = (insets: WindowInsetsCompat, runningAnimations: MutableList<WindowInsetsAnimationCompat>) -> WindowInsetsCompat

val viewWindowInsetsAnimationListenerMap =
  WeakHashMap<View, MutableSet<WindowInsetsAnimationListener>>();

@RequiresApi(Build.VERSION_CODES.R)
fun View.addWindowInsetsAnimationListener(listener: WindowInsetsAnimationListener) =
  viewWindowInsetsAnimationListenerMap.getOrPut(this) {
    mutableSetOf<WindowInsetsAnimationListener>().also { listeners ->
      ViewCompat.setWindowInsetsAnimationCallback(
        this,
        object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
          override fun onProgress(
            insets: WindowInsetsCompat, runningAnimations: MutableList<WindowInsetsAnimationCompat>
          ): WindowInsetsCompat {
            var insetsCompat = insets
            synchronized(listeners) {
              for (cb in listeners) {
                insetsCompat = cb(insetsCompat, runningAnimations)
              }
            }
            return insetsCompat
          }
        })
    }
  }.also { synchronized(it) { it.add(listener) } }.let {
    {
      synchronized(it) { it.remove(listener) }
    }
  }