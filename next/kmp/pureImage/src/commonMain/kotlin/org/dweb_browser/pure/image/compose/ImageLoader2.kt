package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalPlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.NetworkFetcher
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.content.OutgoingContent
import io.ktor.util.InternalAPI
import io.ktor.util.date.GMTDate
import io.ktor.util.flattenEntries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureServerRequest
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.pure.image.offscreenwebcanvas.FetchHook
import org.dweb_browser.pure.image.offscreenwebcanvas.FetchHookContext
import org.dweb_browser.pure.image.removeOriginAndAcceptEncoding
import kotlin.coroutines.CoroutineContext


class ImageLoader2(private val diskCache: DiskCache? = null) {
  @Composable
  fun Load(
    url: String, maxWidth: Dp, maxHeight: Dp, hook: FetchHook? = null
  ): ImageLoadResult {
    val density = LocalDensity.current.density
    val containerWidth = (maxWidth.value * density).toInt()
    val containerHeight = (maxHeight.value * density).toInt()
    return Load(url, containerWidth, containerHeight, hook)
  }

  @OptIn(ExperimentalCoilApi::class)
  @Composable
  fun Load(
    url: String, containerWidth: Int, containerHeight: Int, hook: FetchHook? = null
  ): ImageLoadResult {

    val safeHook: FetchHook? = remember(hook) {
      hook?.let {
        {
          when (url) {
            url -> hook()
            else -> null
          }
        }
      }
    }

    if (safeHook != null) {
      hooks.add(safeHook)
    }

    val loader =
      coilImageLoader ?: GetLoader(hooks, diskCache).also { coilImageLoader = it }

    val platformContext = LocalPlatformContext.current
    return produceState(ImageLoadResult.Setup) {
      value = ImageLoadResult.Loading;
      value = when (val result = loader.execute(ImageRequest.Builder(platformContext).run {
        data(url)
        build()
      })) {
        is ErrorResult -> ImageLoadResult.error(result.throwable)
        is SuccessResult -> ImageLoadResult.success(
          result.image.toImageBitmap()
        )
      }
      if (safeHook != null) {
        hooks.remove(safeHook)
      }
    }.value
  }

  private val hooks = mutableSetOf<FetchHook>()

  private var coilImageLoader: ImageLoader? = null


  companion object {
    @Composable
    fun GetLoader(hooks: Set<FetchHook>? = null, diskCache: DiskCache? = null): ImageLoader {
      val platformContext = LocalPlatformContext.current
      val scope = rememberCoroutineScope()
      val cil = remember(platformContext, hooks, diskCache) {
        ImageLoader.Builder(platformContext).components {
          val defaultHttpClient = lazy { HttpClient() }
          if (hooks == null) {
            add(NetworkFetcher.Factory(defaultHttpClient))
          } else {
            add(NetworkFetcher.Factory(lazy {
              HttpClient(engine = object : HttpClientEngine {
                override val config: HttpClientEngineConfig = HttpClientEngineConfig()
                override val dispatcher: CoroutineDispatcher = Dispatchers.IO
                val job = SupervisorJob()

                @InternalAPI
                override suspend fun execute(data: HttpRequestData): HttpResponseData {
                  for (hook in hooks) {
                    val pureResponse = FetchHookContext(
                      PureServerRequest(
                        data.url.toString(),
                        PureMethod.from(data.method),
                        PureHeaders(data.headers.flattenEntries().removeOriginAndAcceptEncoding()),
                        when (val body = data.body) {
                          is OutgoingContent.ByteArrayContent -> IPureBody.from(body.bytes())
                          is OutgoingContent.NoContent -> IPureBody.Empty
                          is OutgoingContent.ProtocolUpgrade -> throw Exception("no support ProtocolUpgrade")
                          is OutgoingContent.ReadChannelContent -> IPureBody.from(PureStream(body.readFrom()))
                          is OutgoingContent.WriteChannelContent -> throw Exception("no support WriteChannelContent")
                        }
                      ),
                    ).hook() ?: continue
                    return HttpResponseData(
                      statusCode = pureResponse.status,
                      headers = Headers.build {
                        for ((key, value) in pureResponse.headers) {
                          this.append(key, value)
                        }
                      },
                      body = pureResponse.body.toPureStream().getReader("to HttpResponseData"),
                      version = HttpProtocolVersion.HTTP_1_1,
                      requestTime = GMTDate(null),
                      callContext = coroutineContext
                    )
                  }
                  return defaultHttpClient.value.engine.execute(data)
                }

                override fun close() {
                  job.cancel()
                }

                override val coroutineContext: CoroutineContext = scope.coroutineContext + job
              }) { }
            }))
          }
          add(SvgDecoder.Factory())
        }
          .memoryCache {
            MemoryCache.Builder()
              // Set the max size to 25% of the app's available memory.
              .maxSizePercent(platformContext, percent = 0.25)
              .build()
          }
          .diskCache(diskCache)
          // Show a short crossfade when loading images asynchronously.
          .crossfade(true)
          .build()
      }
      return cil
    }

    @Composable
    fun GetImageLoader2(): ImageLoader2 {
      return remember(null) { ImageLoader2(null) }
    }
  }


}

