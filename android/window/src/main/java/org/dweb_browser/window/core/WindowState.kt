package org.dweb_browser.window.core

import androidx.compose.ui.graphics.Color
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.android.toHex
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.help.MMID
import org.dweb_browser.microservice.help.gson
import org.dweb_browser.window.core.constant.UUID
import org.dweb_browser.window.core.constant.WindowBottomBarTheme
import org.dweb_browser.window.core.constant.WindowMode
import org.dweb_browser.window.core.constant.WindowPropertyKeys
import org.dweb_browser.window.core.constant.debugWindow
import java.lang.reflect.Type


/**
 * 单个窗口的信息集合
 */
@JsonAdapter(WindowState::class)
class WindowState(
  /// 这里是窗口的不可变信息
  /**
   * 窗口全局唯一编号，属于UUID的格式
   */
  val wid: UUID = java.util.UUID.randomUUID().toString(),
  /**
   * 窗口持有者
   *
   * 窗口创建者
   */
  val owner: MMID,
  /**
   * 内容提提供方
   *
   * 比如若渲染的是web内容，那么应该是 mwebview.browser.dweb
   */
  val provider: MMID,
  /**
   * 提供放的 mm 实例
   */
  val microModule: MicroModule? = null,
) : JsonSerializer<WindowState>, JsonDeserializer<WindowState> {
  /**
   * 以下是可变属性，所以这里提供一个监听器，来监听所有的属性变更
   */
  val observable = Observable<WindowPropertyKeys>();
  fun toJsonAble() = JsonObject().also { jsonObject ->
    jsonObject.addProperty("wid", wid)
    jsonObject.addProperty("owner", owner)
    jsonObject.addProperty("provider", provider)
    for (ob in observable.observers) {
      val key = ob.key.fieldName
      when (val value = ob.value) {
        is String -> jsonObject.addProperty(key, value)
        is Number -> jsonObject.addProperty(key, value)
        is Boolean -> jsonObject.addProperty(key, value)
        is WindowBounds -> jsonObject.add(key, gson.toJsonTree(value))
        else -> {
          debugWindow("WindowState.toJsonAble", "fail for key:$key")
        }
      }
    }
  }


  override fun serialize(
    src: WindowState, typeOfSrc: Type, context: JsonSerializationContext
  ) = JsonObject().also { jsonObject ->
    jsonObject.addProperty("wid", wid)
    jsonObject.addProperty("owner", owner)
    jsonObject.addProperty("provider", provider)
    for (ob in observable.observers) {
      jsonObject.add(ob.key.fieldName, context.serialize(ob.value, ob.valueClass.java))
    }
  }

  override fun deserialize(
    json: JsonElement, typeOfT: Type, context: JsonDeserializationContext
  ) = json.asJsonObject.let { jsonObject ->
    WindowState(
      jsonObject.get("wid").asString,
      jsonObject.get("owner").asString,
      jsonObject.get("provider").asString,
    ).also { windowState ->
      for (ob in observable.observers) {
        val key = ob.key.fieldName
        if (jsonObject.has(key)) {
          val ele = jsonObject.get(key)
          (ob as Observable.Observer<WindowPropertyKeys, Any>).set(
            context.deserialize(
              ele, ob.valueClass.java
            )
          )
        }
      }
    }
  }


  /**
   * 窗口位置和大小
   *
   * 窗口会被限制最小值,会被限制显示区域。
   * 终止,窗口最终会被绘制在用户可见可控的区域中
   */
  var bounds by observable.observe(
    WindowPropertyKeys.Bounds,
    WindowBounds(),
  );

  fun updateBounds(updater: WindowBounds.() -> WindowBounds) =
    updater.invoke(bounds).also { bounds = it }

  fun updateMutableBounds(updater: WindowBounds.Mutable.() -> Unit) =
    bounds.toMutable().also(updater).also { bounds = it.toImmutable() }

  /**
   * 键盘插入到内容底部的高度
   */
  var keyboardInsetBottom by observable.observe(
    WindowPropertyKeys.KeyboardInsetBottom,
    0f,
  );

  /**
   * 键盘是否可以覆盖内容显示
   * 默认是与内容有交集的，宁愿去 resize content 也不能覆盖
   */
  var keyboardOverlaysContent by observable.observe(
    WindowPropertyKeys.KeyboardOverlaysContent,
    false,
  );

  /**
   * 窗口标题
   *
   * 该标题不需要一定与应用名称相同
   *
   * 如果是 mwebview，默认会采用当前 Webview 的网页 title
   */
  var title by observable.observeNullable(WindowPropertyKeys.Title, String::class);

  /**
   * 应用图标链接
   *
   * 该链接与应用图标不同
   *
   * 如果是 mwebview，默认会采用当前 Webview 的网页 favicon
   */
  var iconUrl by observable.observeNullable(WindowPropertyKeys.IconUrl, String::class);

  /**
   * 图标是否可被裁切，默认不可裁切
   *
   * 如果你的图标自带安全区域，请标记成true
   * （可以用圆形来作为图标的遮罩，如果仍然可以正确显示，那么就属于 maskable=true）
   */
  var iconMaskable by observable.observe(WindowPropertyKeys.IconMaskable, false);

  /**
   * 图标是否单色
   *
   * 如果是单色调，那么就会被上下文所影响，从而在不同的场景里会被套上不同的颜色
   */
  var iconMonochrome by observable.observe(WindowPropertyKeys.IconMonochrome, false);

  /**
   * 是否全屏
   */
  var mode by observable.observe<WindowMode>(WindowPropertyKeys.Mode, WindowMode.FLOATING);

  /**
   * 导航是否可以后退
   *
   * 可空，如果为空，那么禁用返回按钮
   */
  var canGoBack by observable.observeNullable(
    WindowPropertyKeys.CanGoBack, Boolean::class, false
  );

  /**
   * 导航是否可以前进
   *
   * 可空，如果为空，那么禁用前进按钮
   */
  var canGoForward by observable.observeNullable(
    WindowPropertyKeys.CanGoForward, Boolean::class, null
  );

  /**
   * 当前是否缩放窗口
   */
  var resizable by observable.observe<Boolean>(WindowPropertyKeys.Resizable, true);

  /**
   * 是否聚焦
   *
   * 目前只会有一个窗口被聚焦,未来实现多窗口联合显示的时候,就可能会有多个窗口同时focus,但这取决于所处宿主操作系统的支持。
   */
  var focus by observable.observe<Boolean>(WindowPropertyKeys.Focus, false);

  /**
   * 当前窗口层叠顺序
   */
  var zIndex by observable.observe<Int>(WindowPropertyKeys.ZIndex, 0);

  /**
   * 子窗口
   */
  var children by observable.observe<List<UUID>>(WindowPropertyKeys.Children, emptyList());

  /**
   * 父窗口
   */
  var parent by observable.observeNullable(WindowPropertyKeys.Parent, UUID::class);

  /**
   * 是否在闪烁提醒
   *
   * > 类似 macos 中的图标弹跳、windows 系统中的窗口闪烁。
   * 在 taskbar 中, running-dot 会闪烁变色
   */
  var flashing by observable.observe<Boolean>(WindowPropertyKeys.Flashing, false);

  /**
   * 闪烁的颜色(格式为： `#RRGGBB[AA]`)
   *
   * 可以通过接口配置该颜色
   */
  var flashColor by observable.observe<String>(
    WindowPropertyKeys.FlashColor, Color.White.toHex(true)
  );

  /**
   * 进度条
   *
   * 范围为 `[0~1]`
   * 如果小于0(通常为 -1),那么代表没有进度条信息,否则将会在taskbar中显示它的进度信息
   */
  var progressBar by observable.observe<Float>(WindowPropertyKeys.ProgressBar, -1f);

  /**
   * 是否置顶显示
   *
   * 这与 zIndex 不冲突,置顶只是一个优先渲染的层级,可以简单理解成 `zIndex+1000`
   *
   * > 前期我们应该不会在移动设备上开放这个接口,因为移动设备的可用空间非常有限,如果允许任意窗口置顶,那么用户体验将会非常糟。
   * > 如果需要置顶功能,可以考虑使用 pictureInPicture
   */
  var alwaysOnTop by observable.observe<Boolean>(WindowPropertyKeys.AlwaysOnTop, false);

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
  var desktopIndex by observable.observe<Int>(WindowPropertyKeys.DesktopIndex, 1);

  /**
   * 当前窗口所在的屏幕 编号
   *
   * > 配合 getScreens 接口,就能获得当前屏幕的详细信息。参考 [`Electron.screen.getAllDisplays(): Electron.Display[]`](https://electronjs.org/docs/api/structures/display)
   * > 未来实现多设备互联时,可以实现窗口的多设备流转
   * > 屏幕与桌面是两个独立的概念
   *
   * 默认是 -1，意味着使用“主桌面”
   */
  var screenId by observable.observe<Int>(WindowPropertyKeys.ScreenId, -1);

  /**
   * 内容渲染是否要覆盖 顶部栏
   */
  var topBarOverlay by observable.observe<Boolean>(WindowPropertyKeys.TopBarOverlay, false);

  /**
   * 内容渲染是否要覆盖 底部栏
   */
  var bottomBarOverlay by observable.observe<Boolean>(WindowPropertyKeys.BottomBarOverlay, false);

  /**
   * 应用的主题色，格式为 #RRGGBB ｜ auto
   *
   * 如果使用 auto，则会根据当前的系统的显示模式，自动跟随成 亮色 或者 暗色
   */
  var themeColor by observable.observe<String>(WindowPropertyKeys.ThemeColor, "auto");

  /**
   * 顶部栏的文字颜色，格式为 #RRGGBB | auto
   *
   * 如果使用 auto，会自动根据现有的背景色来显示 亮色 或者 暗色
   */
  var topBarContentColor by observable.observe<String>(
    WindowPropertyKeys.TopBarContentColor, "auto"
  );

  /**
   * 顶部栏的文字颜色，格式为 #RRGGBB ｜ auto
   *
   * 如果使用 auto，会与 themeColor 保持一致
   */
  var topBarBackgroundColor by observable.observe<String>(
    WindowPropertyKeys.TopBarBackgroundColor, "auto"
  );

  /**
   * 底部栏的文字颜色，格式为 #RRGGBB | auto
   *
   * 如果使用 auto，会自动根据现有的背景色来显示 亮色 或者 暗色
   */
  var bottomBarContentColor by observable.observe(
    WindowPropertyKeys.BottomBarContentColor,
    "auto",
  );

  /**
   * 底部栏的文字颜色，格式为 #RRGGBB ｜ auto
   *
   * 如果使用 auto，会与 themeColor 保持一致
   */
  var bottomBarBackgroundColor by observable.observe<String>(
    WindowPropertyKeys.BottomBarBackgroundColor, "auto"
  );

  /**
   * 底部栏的风格，默认是导航模式
   */
  var bottomBarTheme by observable.observe<WindowBottomBarTheme>(
    WindowPropertyKeys.BottomBarTheme, WindowBottomBarTheme.Navigation
  );

  /**
   * 窗口关闭的提示信息
   */
  var closeTip by observable.observeNullable(WindowPropertyKeys.CloseTip, String::class, null);

  /**
   * 是否在显示窗口提示信息
   *
   * PS：开发者可以监听这个属性，然后动态地去修改 closeTip。如果要禁用这种行为，可以将 showCloseTip 的类型修改成 String?
   */
  var showCloseTip by observable.observe(WindowPropertyKeys.ShowCloseTip, false);
}

