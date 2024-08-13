package org.dweb_browser.sys.window.core

//import kotlinx.serialization.internal.NoOpEncoder.encodeSerializableElement
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.PureRect
import org.dweb_browser.sys.window.core.constant.WindowConstants
import org.dweb_browser.sys.window.core.constant.WindowPropertyField
import org.dweb_browser.sys.window.core.constant.WindowPropertyKeys

@Suppress("UNCHECKED_CAST")
object WindowStateSerializer : KSerializer<WindowState> {
  override val descriptor = buildClassSerialDescriptor("WindowState") {
    for ((_, field) in WindowPropertyField.ALL_KEYS) {
      element(field.fieldKey.fieldName, field.descriptor, field.annotations, field.isOptional)
    }
  }

  override fun deserialize(decoder: Decoder): WindowState = decoder.decodeStructure(descriptor) {
    WindowState(WindowConstants("", "", "")).run {
      val observers = observable.observers
      mainLoop@ while (true) {
        when (val idx = decodeElementIndex(descriptor)) {
          CompositeDecoder.DECODE_DONE -> {
            break@mainLoop
          }

          CompositeDecoder.UNKNOWN_NAME -> {
            continue@mainLoop
          }

          0 -> _constants = decodeSerializableElement(descriptor, idx, WindowConstants.serializer())
          else -> {

            val field = WindowPropertyField.ALL_KEYS[idx] ?: continue@mainLoop
            val ob = observers[field.fieldKey] as Observable.Observer<WindowPropertyKeys, Any?>?
              ?: continue@mainLoop

            ob.set(decodeSerializableElement(descriptor, idx, field.serializer as KSerializer<Any>))
          }
        }
      }
      this
    }
  }

  @OptIn(ExperimentalSerializationApi::class)
  override fun serialize(encoder: Encoder, value: WindowState) =
    encoder.encodeStructure(descriptor) {
      encodeSerializableElement(descriptor, 0, WindowConstants.serializer(), value.constants)
      val observers = value.observable.observers
      for ((i, field) in WindowPropertyField.ALL_KEYS) {
        val key = field.fieldKey
        val ob = observers[key] ?: continue
        if (field.isOptional) {
          if (ob.value == null) {
            continue
          }
          encodeNullableSerializableElement(
            descriptor, i, field.serializer as KSerializer<Any>, ob.value
          )
        } else {
          encodeSerializableElement(
            descriptor, i, field.serializer as KSerializer<Any>, ob.value!!
          )
        }
      }
    }
}

/**
 * 单个窗口的信息集合
 */
@Serializable(with = WindowStateSerializer::class)
class WindowState(internal var _constants: WindowConstants) {
  val constants get() = _constants

  /**
   * 以下是可变属性，所以这里提供一个监听器，来监听所有的属性变更
   */
  @Transient
  val observable = Observable<WindowPropertyKeys>();

  /**
   * 窗口位置和大小
   *
   * 窗口会被限制最小值,会被限制显示区域。
   * 终止,窗口最终会被绘制在用户可见可控的区域中
   */
  var bounds by WindowPropertyField.WindowBounds.toObserve(observable)
    private set


  /**
   * 更新 窗口位置和大小 的因缘
   * 有两种因缘：
   * 一种是 通过 内部进行触发
   * 一种是 通过 外部进行触发
   *
   * 在原生窗口模式下，我们需要将标准窗口与原生窗口进行双向绑定。
   * 为了避免双向绑定带来的抖动为题，这里需要标记绑定方向
   */
  var updateBoundsReason = UpdateReason.Inner
    private set

  /**
   * 更新的因缘
   * 有两种因缘：
   * 一种是 通过 内部进行触发
   * 一种是 通过 外部进行触发
   *
   * 在原生窗口模式下，我们需要将标准窗口与原生窗口进行双向绑定。
   * 为了避免双向绑定带来的抖动为题，这里需要标记绑定方向
   *
   */
  enum class UpdateReason {
    Inner, Outer,
  }

  /**
   * 如果 reason 是 Outer，那么不会同步给 原生窗口
   */
  fun updateBounds(bounds: PureRect, reason: UpdateReason) {
    if (this.bounds != bounds) {
      this.bounds = bounds
      this.updateBoundsReason = reason
    }
  }

  inline fun updateBounds(
    reason: UpdateReason = UpdateReason.Inner, updater: PureRect.() -> PureRect,
  ) = updater.invoke(bounds).also { updateBounds(it, reason) }

  inline fun updateMutableBounds(
    reason: UpdateReason = UpdateReason.Inner, noinline updater: PureRect.Mutable.() -> Unit,
  ) = bounds.mutable(updater).also { updateBounds(it, reason) }

  /**
   * 窗口渲染相关的配置项目
   */
  internal val renderConfig = WindowRenderConfig()

  /**
   * 键盘插入到内容底部的高度
   */
  var keyboardInsetBottom by WindowPropertyField.KeyboardInsetBottom.toObserve(observable)

  /**updateMutableBounds
   * 键盘是否可以覆盖内容显示
   * 默认是与内容有交集的，宁愿去 resize content 也不能覆盖
   */
  var keyboardOverlaysContent by WindowPropertyField.KeyboardOverlaysContent.toObserve(
    observable
  )

  /**
   * 窗口标题
   *
   * 该标题不需要一定与应用名称相同
   *
   * 如果是 mwebview，默认会采用当前 Webview 的网页 title
   */
  var title by WindowPropertyField.Title.toObserve(observable)

  /**
   * 应用图标链接
   *
   * 该链接与应用图标不同
   *
   * 如果是 mwebview，默认会采用当前 Webview 的网页 favicon
   */
  var iconUrl by WindowPropertyField.IconUrl.toObserve(observable)

  /**
   * 图标是否可被裁切，默认不可裁切
   *
   * 如果你的图标自带安全区域，请标记成true
   * （可以用圆形来作为图标的遮罩，如果仍然可以正确显示，那么就属于 maskable=true）
   */
  var iconMaskable by WindowPropertyField.IconMaskable.toObserve(observable)

  /**
   * 图标是否单色
   *
   * 如果是单色调，那么就会被上下文所影响，从而在不同的场景里会被套上不同的颜色
   */
  var iconMonochrome by WindowPropertyField.IconMonochrome.toObserve(observable)

  /**
   * 窗口显示模式（最大化、全屏、浮动窗口、画中画）
   */
  var mode by WindowPropertyField.Mode.toObserve(observable)

  /**
   * 窗口是否隐藏（最小化）
   */
  var visible by WindowPropertyField.Visible.toObserve(observable)

  /**
   * 窗口是否关闭（销毁）
   */
  var closed by WindowPropertyField.Closed.toObserve(observable)

  /**
   * 导航是否可以后退
   *
   * 可空，如果为空，那么禁用返回按钮
   */
  var canGoBack by WindowPropertyField.CanGoBack.toObserve(observable)

  /**
   * 导航是否可以前进
   *
   * 可空，如果为空，那么禁用前进按钮
   */
  var canGoForward by WindowPropertyField.CanGoForward.toObserve(observable)

  /**
   * 当前是否缩放窗口
   */
  var resizable by WindowPropertyField.Resizable.toObserve(observable)

  /**
   * 是否聚焦
   *
   * 目前只会有一个窗口被聚焦,未来实现多窗口联合显示的时候,就可能会有多个窗口同时focus,但这取决于所处宿主操作系统的支持。
   */
  var focus by WindowPropertyField.Focus.toObserve(observable)

  /**
   * 当前窗口层叠顺序
   */
  var zIndex by WindowPropertyField.ZIndex.toObserve(observable)

  /**
   * 子窗口
   */
  var children by WindowPropertyField.Children.toObserve(observable)

  /**
   * 父窗口
   */
  var parent by WindowPropertyField.Parent.toObserve(observable)

  /**
   * 是否在闪烁提醒
   *
   * > 类似 macos 中的图标弹跳、windows 系统中的窗口闪烁。
   * 在 taskbar 中, running-dot 会闪烁变色
   */
  var flashing by WindowPropertyField.Flashing.toObserve(observable)

  /**
   * 闪烁的颜色(格式为： `#RRGGBB[AA]`)
   *
   * 可以通过接口配置该颜色
   */
  var flashColor by WindowPropertyField.FlashColor.toObserve(observable)

  /**
   * 进度条
   *
   * 范围为 `[0~1]`
   * 如果小于0(通常为 -1),那么代表没有进度条信息,否则将会在taskbar中显示它的进度信息
   */
  var progressBar by WindowPropertyField.ProgressBar.toObserve(observable)

  /**
   * 是否置顶显示
   *
   * 这与 zIndex 不冲突,置顶只是一个优先渲染的层级,可以简单理解成 `zIndex+1000`
   *
   * > 前期我们应该不会在移动设备上开放这个接口,因为移动设备的可用空间非常有限,如果允许任意窗口置顶,那么用户体验将会非常糟。
   * > 如果需要置顶功能,可以考虑使用 pictureInPicture
   */
  var alwaysOnTop by WindowPropertyField.AlwaysOnTop.toObserve(observable)

  /**
   * 是否在窗口都关闭后，仍然保持在后台运行
   */
  var keepBackground by WindowPropertyField.KeepBackground.toObserve(observable)

  /**
   * 当前窗口所属的桌面 编号
   * 目前有 0 和 1 两个桌面,其中 0 为 taskbar 中的 toogleDesktopButton 开关所代表的 “临时桌面”。
   * 目前,点击 toogleDesktopButton 的效果就是将目前打开的窗口都收纳入“临时桌面”;
   * 如果“临时桌面”中存在暂存的窗口,那么此时点击“临时桌面”,这些暂存窗口将恢复到“当前桌面”。
   *
   * 未来会实现将窗口拖拽到“临时桌面”中,这样可以实现在多个桌面中移动窗口
   *
   * 默认是 1
   */
  var desktopIndex by WindowPropertyField.DesktopIndex.toObserve(observable)

  /**
   * 当前窗口所在的屏幕 编号
   *
   * > 配合 getScreens 接口,就能获得当前屏幕的详细信息。参考 [`Electron.screen.getAllDisplays(): Electron.Display[]`](https://electronjs.org/docs/api/structures/display)
   * > 未来实现多设备互联时,可以实现窗口的多设备流转
   * > 屏幕与桌面是两个独立的概念
   *
   * 默认是 -1，意味着使用“主桌面”
   */
  var screenId by WindowPropertyField.ScreenId.toObserve(observable)

  /**
   * 内容渲染是否要覆盖 顶部栏
   */
  var topBarOverlay by WindowPropertyField.TopBarOverlay.toObserve(observable)

  /**
   * 内容渲染是否要覆盖 底部栏
   */
  var bottomBarOverlay by WindowPropertyField.BottomBarOverlay.toObserve(observable)

  /**
   * 应用的主题色，格式为 #RRGGBB ｜ auto
   *
   * 如果使用 auto，则会根据当前的系统的显示模式，自动跟随成 亮色 或者 暗色
   */
  var themeColor by WindowPropertyField.ThemeColor.toObserve(observable)
  var themeDarkColor by WindowPropertyField.ThemeDarkColor.toObserve(observable)

  /**
   * 顶部栏的文字颜色，格式为 #RRGGBB | auto
   *
   * 如果使用 auto，会自动根据现有的背景色来显示 亮色 或者 暗色
   */
  var topBarContentColor by WindowPropertyField.TopBarContentColor.toObserve(observable)
  var topBarContentDarkColor by WindowPropertyField.TopBarContentDarkColor.toObserve(
    observable
  )

  /**
   * 顶部栏的文字颜色，格式为 #RRGGBB ｜ auto
   *
   * 如果使用 auto，会与 themeColor 保持一致
   */
  var topBarBackgroundColor by WindowPropertyField.TopBarBackgroundColor.toObserve(observable)
  var topBarBackgroundDarkColor by WindowPropertyField.TopBarBackgroundDarkColor.toObserve(
    observable
  )

  /**
   * 底部栏的文字颜色，格式为 #RRGGBB | auto
   *
   * 如果使用 auto，会自动根据现有的背景色来显示 亮色 或者 暗色
   */
  var bottomBarContentColor by WindowPropertyField.BottomBarContentColor.toObserve(observable)
  var bottomBarContentDarkColor by WindowPropertyField.BottomBarContentDarkColor.toObserve(
    observable
  )

  /**
   * 底部栏的文字颜色，格式为 #RRGGBB ｜ auto
   *
   * 如果使用 auto，会与 themeColor 保持一致
   */
  var bottomBarBackgroundColor by WindowPropertyField.BottomBarBackgroundColor.toObserve(
    observable
  )
  var bottomBarBackgroundDarkColor by WindowPropertyField.BottomBarBackgroundDarkColor.toObserve(
    observable
  )

  /**
   * 底部栏的风格，默认是导航模式
   */
  var bottomBarTheme by WindowPropertyField.BottomBarTheme.toObserve(observable)

  /**
   * 窗口关闭的提示信息
   *
   * 如果非 null（即便是空字符串），那么窗口关闭前，会提供提示信息
   */
  var closeTip by WindowPropertyField.CloseTip.toObserve(observable)

  /**
   * 是否在显示窗口提示信息
   *
   * PS：开发者可以监听这个属性，然后动态地去修改 closeTip。如果要禁用这种行为，可以将 showCloseTip 的类型修改成 String?
   */
  var showCloseTip by WindowPropertyField.ShowCloseTip.toObserve(observable)

  /**
   * 是否显示菜单面板
   */
  var showMenuPanel by WindowPropertyField.ShowMenuPanel.toObserve(observable)

  /**
   * 配色方案
   */
  var colorScheme by WindowPropertyField.ColorScheme.toObserve(observable)

  /**
   * 模态窗口
   */
  var modals by WindowPropertyField.Modals.toObserve(observable)

  /**
   * 安全距离
   */
  var safePadding by WindowPropertyField.SafePadding.toObserve(observable)
}

// 设置窗口大小，并且可以传递是否需要让用户可以调整窗口大小
@Serializable
data class SetWindowSize(
  val resizable: Boolean = false,
  val width: Float? = null,
  val height: Float? = null,
)
