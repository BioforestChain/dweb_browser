package org.dweb_browser.browser.desk.render


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.desk.DesktopV2Controller
import org.dweb_browser.browser.desk.model.AppMenuModel
import org.dweb_browser.browser.desk.model.AppMenuType
import org.dweb_browser.browser.desk.model.DesktopAppModel
import org.dweb_browser.browser.desk.model.getAppMenuDisplays
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.helper.PureIntBounds
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.compose.NativeBackHandler
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.compose.hoverCursor
import org.dweb_browser.helper.compose.minus
import org.dweb_browser.helper.compose.plus
import org.dweb_browser.helper.compose.timesIntOffset
import org.dweb_browser.helper.compose.timesToInt
import org.dweb_browser.helper.toRect
import org.dweb_browser.sys.window.render.AppLogo


@Composable
internal fun rememberAppMenuPanel(
  desktopController: DesktopV2Controller,
  microModule: NativeMicroModule.NativeRuntime,
) = remember(desktopController, microModule) { AppMenuPanel(desktopController, microModule) }

internal class AppMenuPanel(
  val desktopController: DesktopV2Controller,
  val microModule: NativeMicroModule.NativeRuntime,
) {
  private var cacheApp by mutableStateOf<DesktopAppModel?>(
    null,
    // 不知道为什么， DesktopAppModel 就是得用 === ，而不能是 ==
    policy = referentialEqualityPolicy()
  )
  var safeAreaInsets by mutableStateOf(WindowInsets(0))

  /**
   * 是否打开应用菜单
   */
  var isOpenMenu by mutableStateOf(false)
    private set
  private val menuProgressAni = Animatable(0f)
  val visibilityProgress get() = menuProgressAni.value
  var isOpenDeleteDialog by mutableStateOf(false)
    private set

  val isOpen get() = isOpenMenu || isOpenDeleteDialog

  fun hide() {
    isOpenMenu = false
    isOpenDeleteDialog = false
  }

  fun show(app: DesktopAppModel) {
    this.cacheApp = app
    doHaptics()
    isOpenMenu = true
  }

  fun doQuit(mmid: String) {
    microModule.scopeLaunch(cancelable = true) {
      desktopController.closeApp(mmid)
    }
  }

  fun doDetail(mmid: String) {
    microModule.scopeLaunch(cancelable = true) {
      desktopController.detail(mmid)
    }
  }

  fun doRemove(mmid: String, isWebLink: Boolean) {
    microModule.scopeLaunch(cancelable = true) {
      desktopController.remove(mmid, isWebLink)
    }
  }

  fun doShare(mmid: String) {
    microModule.scopeLaunch(cancelable = true) {
      desktopController.share(mmid)
    }
  }

  fun doHaptics() {
    microModule.scopeLaunch(cancelable = true) {
      microModule.nativeFetch("file://haptics.sys.dweb/vibrateHeavyClick")
    }
  }

  /**
   * 图层是否最终可见
   */
  private var menuLayerVisibility by mutableStateOf(false)


  companion object {
    val appMenuLayerBgAlpha = when {
      canSupportModifierBlur() -> 0.12f
      else -> 0.6f
    }

    val appMenuAlpha = when {
      canSupportModifierBlur() -> 0.5f
      else -> 1f
    }
  }


  @Composable
  fun Render(modifier: Modifier = Modifier) {
    LaunchedEffect(isOpen) {
      menuLayerVisibility = true
      if (isOpen) {
        menuProgressAni.animateTo(1f, deskAniSpec())
      } else {
        menuProgressAni.animateTo(0f, deskAniSpec())
        menuLayerVisibility = false
      }
    }
    val app = this.cacheApp ?: return
    Box(modifier) {
      if (menuLayerVisibility) {
        AppMenuLayer(app)
      }
      AnimatedVisibility(isOpenDeleteDialog) {
        DeskDeleteAlert(app, microModule, onDismissRequest = { hide() }, onConfirm = {
          hide()
          doRemove(app.mmid, app.isWebLink)
        })
      }
    }
  }

  @Composable
  fun AppMenuLayer(app: DesktopAppModel, modifier: Modifier = Modifier) {
    NativeBackHandler(isOpenMenu) {
      hide()
    }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val d = density.density
    val p = visibilityProgress

    BoxWithConstraints(
      modifier.fillMaxSize().background(Color.Black.copy(appMenuLayerBgAlpha * p)).composed {
        // 这里要做到事件穿透，所以不能用 enabled 来控制
        when {
          isOpenMenu -> clickableWithNoEffect { hide() }
          else -> this
        }
      },
      Alignment.TopStart,
    ) {
      val p1 = if (p >= 0.5f) 1f else p * 2
      val p2 = if (p >= 0.5f) (p - 0.5f) * 2 else 0f
      val iconScaleDiff = 0.1f
      val iconScale = 1f + iconScaleDiff * p2
      val iconAlpha = safeAlpha(p1)
      val appOffset = app.offset
      val appSize = app.size

      AppLogo.from(app.icon, fetchHook = microModule.blobFetchHook).toDeskAppIcon().Render(
        Modifier.requiredSize(
          appSize.width.dp,
          appSize.height.dp
        )// 不要用 translationXY 去做变换，会有消失不见的问题
          .offset(appOffset.x.dp, appOffset.y.dp).graphicsLayer {
            scaleX = iconScale
            scaleY = iconScale
            alpha = iconAlpha
          },
      )

      val safeDrawing = WindowInsets.safeDrawing
      val safeWindowBounds = remember(safeDrawing, safeAreaInsets, density, layoutDirection) {
        val safeInsets = safeDrawing.union(safeAreaInsets)
        PureIntBounds(
          left = safeInsets.getLeft(density, layoutDirection),
          right = safeInsets.getRight(density, layoutDirection),
          top = safeInsets.getTop(density),
          bottom = safeInsets.getBottom(density),
        )
      }

      val appBounds = remember(appOffset, appSize) {
        PureRect(
          x = appOffset.x,
          y = appOffset.y,
          width = appSize.width,
          height = appSize.height,
        ).toPureBounds().centerScale(1f + iconScaleDiff)
      }
      var appMenuIntSizeReady by remember { mutableStateOf(false) }
      var appMenuIntSize by remember {
        mutableStateOf(
          Size(
            appBounds.width,
            appBounds.height
          ).timesToInt(d)
        )
      }

      val positionProvider = rememberMenuTooltipPositionProvider()
      val appMenuIntOffset =
        remember(appMenuIntSize, appBounds, safeWindowBounds, layoutDirection) {
          if (!appMenuIntSizeReady) {
            return@remember Offset(appBounds.left, appBounds.top).timesIntOffset(d)
          }
          val windowSize = IntSize(constraints.maxWidth, constraints.maxHeight).minus(
            w = safeWindowBounds.left + safeWindowBounds.right,
            h = safeWindowBounds.top + safeWindowBounds.bottom,
          )
          val anchorBounds = appBounds.toPureRect().toRect().timesToInt(d).minus(
            l = safeWindowBounds.left,
            t = safeWindowBounds.top,
            b = safeWindowBounds.top,
            r = safeWindowBounds.left,
          )
          positionProvider.calculatePosition(
            anchorBounds = anchorBounds,
            windowSize = windowSize,
            layoutDirection = layoutDirection,
            popupContentSize = appMenuIntSize
          ).plus(x = safeWindowBounds.left, y = safeWindowBounds.top)
        }

      val menuAlpha = safeAlpha(p)
      val p3 = 0.9f + p * 0.1f
      val startIntX = appBounds.left * d
      val startIntY = appBounds.top * d
      val endIntX = appMenuIntOffset.x
      val endIntY = appMenuIntOffset.y
      val aniTranslationX = startIntX + (endIntX - startIntX) * p3
      val aniTranslationY = startIntY + (endIntY - startIntY) * p3

      val startScaleX = appSize.width / (appMenuIntSize.width / d)
      val startScaleY = appSize.height / (appMenuIntSize.height / d)
      val aniScaleX = startScaleX + (1f - startScaleX) * p3
      val aniScaleY = startScaleY + (1f - startScaleY) * p3

      AppMenu(
        displays = remember(app) { app.getAppMenuDisplays() },
        modifier = Modifier.offset(
          (aniTranslationX / d).dp,
          (aniTranslationY / d).dp
        )// 不要用 translationXY 去做变换，会有消失不见的问题
          .graphicsLayer {
//            transformOrigin = TransformOrigin(0f, 0f)
//            translationX = aniTranslationX
//            translationY = aniTranslationY
            scaleX = aniScaleX
            scaleY = aniScaleY
            alpha = menuAlpha
          }.onGloballyPositioned {
            appMenuIntSizeReady = true
            appMenuIntSize = it.size
          },
        action = { type ->
          when (type) {
            AppMenuType.OFF -> hide().also { doQuit(app.mmid) }
            AppMenuType.DETAIL -> hide().also { doDetail(app.mmid) }
            AppMenuType.SHARE -> hide().also { doShare(app.mmid) }
            AppMenuType.UNINSTALL, AppMenuType.DELETE -> {
              isOpenMenu = false
              isOpenDeleteDialog = true
            }
          }
        },
      )
    }
  }

  @Composable
  private fun AppMenu(
    displays: List<AppMenuModel>,
    modifier: Modifier,
    action: (AppMenuType) -> Unit,
  ) {
    Box(modifier.background(Color.White.copy(alpha = appMenuAlpha), deskSquircleShape())) {
      Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        displays.forEach { display ->
          TextButton(
            {
              action(display.type)
            },
            enabled = display.enable,
            shape = deskSquircleShape(),
            modifier = Modifier.hoverCursor(),
            colors = ButtonDefaults.textButtonColors().run {
              remember {
                copy(
                  contentColor = display.type.data.color.copy(alpha = 0.9f),
                  disabledContentColor = Color.Black.copy(alpha = 0.4f),
                )
              }
            }
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(display.type.data.icon, null, modifier = Modifier.size(24.dp))
              Text(display.type.data.title, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }
  }
}
