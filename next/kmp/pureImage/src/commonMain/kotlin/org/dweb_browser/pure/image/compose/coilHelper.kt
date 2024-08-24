package org.dweb_browser.pure.image.compose

import androidx.compose.ui.graphics.ImageBitmap
import coil3.Image
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.annotation.ExperimentalCoilApi
import coil3.svg.SvgDecoder
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut

val contextImageLoaderCache = WeakHashMap<PlatformContext, ImageLoader>()
val synchronizedObject by lazy { SynchronizedObject() }
fun PlatformContext.getCoilImageLoader() = synchronized(synchronizedObject) {
  contextImageLoaderCache.getOrPut(this) {
    ImageLoader(this).newBuilder().components {
      add(SvgDecoder.Factory())
      addPlatformComponents()
    }.build()
  }
}

@OptIn(ExperimentalCoilApi::class)
expect fun Image.toImageBitmap(): ImageBitmap