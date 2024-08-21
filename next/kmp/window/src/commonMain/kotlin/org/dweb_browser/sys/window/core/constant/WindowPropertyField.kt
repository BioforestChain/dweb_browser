package org.dweb_browser.sys.window.core.constant

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.serializer
import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.PureBounds
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.compose.toHex
import org.dweb_browser.sys.window.core.modal.ModalState
import kotlin.reflect.KClass

/**
 * 可变属性名称集合
 */
open class WindowPropertyField<T : Any> private constructor(
  val fieldKey: WindowPropertyKeys,
  val fieldType: KClass<T>,
  val serializer: KSerializer<T>,
  val isOptional: Boolean,
  val initValue: T?,
) {
  val descriptor = serializer.descriptor

  @OptIn(ExperimentalSerializationApi::class)
  val annotations = descriptor.annotations

  class Required<T : Any>(
    fieldKey: WindowPropertyKeys,
    fieldType: KClass<T>,
    serializer: KSerializer<T>,
    initValue: T,
  ) : WindowPropertyField<T>(fieldKey, fieldType, serializer, false, initValue) {

    fun toObserve(observable: Observable<WindowPropertyKeys>) =
      observable.observe(this.fieldKey, initValue!!)
  }

  class Optional<T : Any>(
    fieldKey: WindowPropertyKeys,
    fieldType: KClass<T>,
    serializer: KSerializer<T>,
    initValue: T?,
  ) : WindowPropertyField<T>(fieldKey, fieldType, serializer, true, initValue) {

    fun toObserve(observable: Observable<WindowPropertyKeys>) =
      observable.observeNullable(this.fieldKey, initValue)
  }

  init {
    _ALL_VALUES.add(this)
  }

  companion object {

    @OptIn(InternalSerializationApi::class)
    private inline fun <reified T : Any> required(
      fieldKey: WindowPropertyKeys,
      initValue: T,
      serializer: KSerializer<T> = T::class.serializer(),
    ) = Required(fieldKey, T::class, serializer, initValue)

    @OptIn(InternalSerializationApi::class)
    private inline fun <reified T : Any> optional(
      fieldKey: WindowPropertyKeys,
      initValue: T? = null,
      serializer: KSerializer<T> = T::class.serializer(),
    ) = Optional(fieldKey, T::class, serializer, initValue)

    private val _ALL_VALUES = mutableListOf<WindowPropertyField<*>>()
    val ALL_KEYS by lazy { _ALL_VALUES.mapIndexed { index, field -> index to field }.toMap() }
    val ALL_VALUES by lazy { _ALL_VALUES.associateBy { it.fieldKey } }

    val Constants = required(WindowPropertyKeys.Constants, WindowConstants("", "", ""))
    val Title = optional<String>(WindowPropertyKeys.Title)
    val IconUrl = optional<String>(WindowPropertyKeys.IconUrl)
    val IconMaskable = required(WindowPropertyKeys.IconMaskable, false)
    val IconMonochrome = required(WindowPropertyKeys.IconMonochrome, false)
    val Mode = required(WindowPropertyKeys.Mode, WindowMode.FLOAT)
    val Visible = required(WindowPropertyKeys.Visible, true)
    val Closed = required(WindowPropertyKeys.Closed, false)
    val CanGoBack = optional<Boolean>(WindowPropertyKeys.CanGoBack, false)
    val CanGoForward = optional<Boolean>(WindowPropertyKeys.CanGoForward)
    val Resizable = required(WindowPropertyKeys.Resizable, true)
    val Focus = required(WindowPropertyKeys.Focus, false)
    val ZIndex = required(WindowPropertyKeys.ZIndex, 0)
    val Children = required<List<UUID>>(
      WindowPropertyKeys.Children, listOf(), serializer = ListSerializer(String.serializer())
    )
    val Parent = optional<String>(WindowPropertyKeys.Parent)
    val Flashing = required(WindowPropertyKeys.Flashing, false)
    val FlashColor = required(WindowPropertyKeys.FlashColor, Color.White.toHex(true))
    val ProgressBar = required(WindowPropertyKeys.ProgressBar, -1f)
    val AlwaysOnTop = required(WindowPropertyKeys.AlwaysOnTop, false)
    val KeepBackground = required(WindowPropertyKeys.KeepBackground, false)
    val DesktopIndex = required(WindowPropertyKeys.DesktopIndex, 1)
    val ScreenId = required(WindowPropertyKeys.ScreenId, -1)
    val TopBarOverlay = required(WindowPropertyKeys.TopBarOverlay, false)
    val BottomBarOverlay = required(WindowPropertyKeys.BottomBarOverlay, false)
    val TopBarContentColor = required(WindowPropertyKeys.TopBarContentColor, "auto")
    val TopBarContentDarkColor = required(WindowPropertyKeys.TopBarContentDarkColor, "auto")
    val TopBarBackgroundColor = required(WindowPropertyKeys.TopBarBackgroundColor, "auto")
    val TopBarBackgroundDarkColor = required(WindowPropertyKeys.TopBarBackgroundDarkColor, "auto")
    val BottomBarContentColor = required(WindowPropertyKeys.BottomBarContentColor, "auto")
    val BottomBarContentDarkColor = required(WindowPropertyKeys.BottomBarContentDarkColor, "auto")
    val BottomBarBackgroundColor = required(WindowPropertyKeys.BottomBarBackgroundColor, "auto")
    val BottomBarBackgroundDarkColor =
      required(WindowPropertyKeys.BottomBarBackgroundDarkColor, "auto")
    val BottomBarTheme =
      required(WindowPropertyKeys.BottomBarTheme, WindowBottomBarTheme.Navigation)
    val ThemeColor = required(WindowPropertyKeys.ThemeColor, "auto")
    val ThemeDarkColor = required(WindowPropertyKeys.ThemeDarkColor, "auto")
    val WindowBounds = required(WindowPropertyKeys.Bounds, PureRect())
    val KeyboardInsetBottom = required(WindowPropertyKeys.KeyboardInsetBottom, 0f)
    val KeyboardOverlaysContent = required(WindowPropertyKeys.KeyboardOverlaysContent, false)
    val CloseTip = optional<String>(WindowPropertyKeys.CloseTip)
    val ShowCloseTip = required(WindowPropertyKeys.ShowCloseTip, false)
    val ShowMenuPanel = required(WindowPropertyKeys.ShowMenuPanel, false)
    val ColorScheme = required(WindowPropertyKeys.ColorScheme, WindowColorScheme.Normal)
    val Modals = required<Map<String, ModalState>>(
      WindowPropertyKeys.Modals,
      mapOf(),
      serializer = MapSerializer(String.serializer(), ModalState.serializer())
    )
    val SafePadding = required(WindowPropertyKeys.SafePadding, PureBounds.Zero)
  }

}