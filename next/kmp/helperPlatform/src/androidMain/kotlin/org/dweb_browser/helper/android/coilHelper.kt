package org.dweb_browser.helper.android

import android.content.Context
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.SvgDecoder
import java.util.WeakHashMap

val contextImageLoaderCache = WeakHashMap<Context, ImageLoader>()
fun Context.getCoilImageLoader() = synchronized(this) {
  contextImageLoaderCache.getOrPut(this) {
    ImageLoader(this).newBuilder().components {
      add(SvgDecoder.Factory())
      add(GifDecoder.Factory())
    }.build()
  }
}