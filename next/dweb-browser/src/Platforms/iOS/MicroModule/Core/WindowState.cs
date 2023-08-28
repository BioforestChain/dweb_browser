using System.Text.Json.Serialization;
using UIKit;

namespace DwebBrowser.MicroService.Core;

/// <summary>
/// 单个窗口的状态集
/// </summary>
public class WindowState
{
    #region 窗口不可变信息

    /// <summary>
    /// 窗口全局唯一编号，属于UUID的格式
    /// </summary>
    [JsonPropertyName("wid")]
    public UUID Wid { get; init; }

    /// <summary>
    /// 窗口持有者
    ///
    /// 窗口创建者
    /// </summary>
    [JsonPropertyName("owner")]
    public Mmid Owner { get; init; }

    /// <summary>
    /// 内容提提供方
    ///
    /// 比如若渲染的是web内容，那么应该是 mwebview.browser.dweb
    /// </summary>
    [JsonPropertyName("provider")]
    public Mmid Provider { get; init; }

    /// <summary>
    /// 提供放的 mm 实例
    /// </summary>
    private MicroModule? MicroModule { get; init; }

    #endregion

    #region 以下是可变属性，所以这里提供一个监听器，来监听所有的属性变更

    public Observable Observable = new();

    /// <summary>
    /// 窗口标题
    ///
    /// 该标题不需要一定与应用名称相同
    ///
    /// 如果是 mwebview，默认会采用当前 Webview 的网页 title
    /// </summary>
    [JsonPropertyName("title")]
    public string? Title { get => title.Get(); set => title.Set(value); }
    private Observable.Observer title { get; init; }

    /// <summary>
    /// 应用图标链接
    ///
    /// 该链接与应用图标不同
    ///
    /// 如果是 mwebview，默认会采用当前 Webview 的网页 favicon
    /// </summary>
    [JsonPropertyName("iconUrl")]
    public string? IconUrl { get => iconUrl.Get(); set => iconUrl.Set(value); }
    private Observable.Observer iconUrl { get; init; }

    /// <summary>
    /// 图标是否可被裁切，默认不可裁切
    ///
    /// 如果你的图标自带安全区域，请标记成true
    /// （可以用圆形来作为图标的遮罩，如果仍然可以正确显示，那么就属于 maskable=true）
    /// </summary>
    [JsonPropertyName("iconMaskable")]
    public bool IconMaskable { get => iconMaskable.Get(); set => iconMaskable.Set(value); }
    private Observable.Observer iconMaskable { get; init; }

    /// <summary>
    /// 图标是否单色
    ///
    /// 如果是单色调，那么就会被上下文所影响，从而在不同的场景里会被套上不同的颜色
    /// </summary>
    [JsonPropertyName("iconMonochrome")]
    public bool IconMonochrome { get => iconMonochrome.Get(); set => iconMonochrome.Set(value); }
    private Observable.Observer iconMonochrome { get; init; }

    /// <summary>
    /// 窗口位置和大小
    ///
    /// 窗口会被限制最小值，会被限制显示区域。
    /// 终止，窗口最终会被绘制在用户可见可控的区域中
    /// </summary>
    [JsonPropertyName("bounds")]
    public WindowBounds Bounds { get => bounds.Get(); set => bounds.Set(value); }
    private Observable.Observer bounds { get; init; }

    public WindowBounds UpdateBounds(WindowBounds newBounds)
    {
        if (!Bounds.Equals(newBounds))
        {
            Bounds = newBounds;
        }

        return Bounds;
    }

    /// <summary>
    /// 窗口模式
    /// </summary>
    [JsonPropertyName("mode")]
    public WindowMode Mode { get => mode.Get(); set => mode.Set(value); }
    private Observable.Observer mode { get; init; }

    /// <summary>
    /// 导航是否可以后退
    ///
    /// 可空，如果为空，那么禁用返回按钮
    /// </summary>
    [JsonPropertyName("canGoBack")]
    public bool CanGoBack { get => canGoBack.Get(); set => canGoBack.Set(value); }
    private Observable.Observer canGoBack { get; init; }

    /// <summary>
    /// 导航是否可以前进
    ///
    /// 可空，如果为空，那么禁用前进按钮
    /// </summary>
    [JsonPropertyName("canGoForward")]
    public bool? CanGoForward { get => canGoForward.Get(); set => canGoForward.Set(value); }
    private Observable.Observer canGoForward { get; init; }

    /// <summary>
    /// 当前是否缩放窗口
    /// </summary>
    [JsonPropertyName("resizable")]
    public bool Resizable { get => resizable.Get(); set => resizable.Set(value); }
    private Observable.Observer resizable { get; init; }

    /// <summary>
    /// 是否聚焦
    ///
    /// 目前只会有一个窗口被聚焦，未来实现多窗口联合显示的时候，就可能会有多个窗口同时focus，但这取决于所处宿主操作系统的支持。
    /// </summary>
    [JsonPropertyName("focus")]
    public bool Focus { get => focus.Get(); set => focus.Set(value); }
    private Observable.Observer focus { get; init; }

    /// <summary>
    /// 当前窗口层叠顺序
    /// @types {float} 这里使用float，本质上意味着这个zIndex是一个“二维”值
    ///
    /// 通常默认情况下都是整数。
    /// 如果一个窗口有了子窗口，那么这个 父窗口 和 这些 子窗口 会使用小数来区分彼此；同时父窗口总是为整数
    /// 这里没有将它拆分成两个数来存储，目的是复合直觉
    /// </summary>
    [JsonPropertyName("zIndex")]
    public int ZIndex { get => zIndex.Get(); set => zIndex.Set(value); }
    private Observable.Observer zIndex { get; init; }

    /// <summary>
    /// 子窗口
    /// </summary>
    [JsonPropertyName("children")]
    public List<UUID> Children { get => children.Get(); set => children.Set(value); }
    private Observable.Observer children { get; init; }

    /// <summary>
    /// 父窗口
    /// </summary>
    [JsonPropertyName("parent")]
    public UUID? Parent { get => parent.Get(); set => parent.Set(value); }
    private Observable.Observer parent { get; init; }

    /// <summary>
    /// 是否在闪烁提醒
    /// </summary>
    [JsonPropertyName("flashing")]
    public bool Flashing { get => flashing.Get(); set => flashing.Set(value); }
    private Observable.Observer flashing { get; init; }

    /// <summary>
    /// > 类似 macos 中的图标弹跳、windows 系统中的窗口闪烁。
    /// 在 taskbar 中， running-dot 会闪烁变色
    /// </summary>
    [JsonPropertyName("flashColor")]
    public string? FlashColor { get => flashColor.Get(); set => flashColor.Set(value); }
    private Observable.Observer flashColor { get; init; }

    /// <summary>
    /// 进度条
    /// 
    /// 范围为 `[0～1]`
    /// 如果小于0（通常为 -1），那么代表没有进度条信息，否则将会在taskbar中显示它的进度信息
    /// </summary>
    [JsonPropertyName("progressBar")]
    public float ProgressBar { get => progressBar.Get(); set => progressBar.Set(value); }
    private Observable.Observer progressBar { get; init; }

    /// <summary>
    /// 是否置顶显示
    ///
    /// 这与 zIndex 不冲突，置顶只是一个优先渲染的层级，可以简单理解成 `zIndex+1000`
    ///
    /// > 前期我们应该不会在移动设备上开放这个接口，因为移动设备的可用空间非常有限，如果允许任意窗口置顶，那么用户体验将会非常糟。
    /// > 如果需要置顶功能，可以考虑使用 pictureInPicture
    /// </summary>
    [JsonPropertyName("alwaysOnTop")]
    public bool AlwaysOnTop { get => alwaysOnTop.Get(); set => alwaysOnTop.Set(value); }
    private Observable.Observer alwaysOnTop { get; init; }

    /// <summary>
    /// 当前窗口所属的桌面
    /// 目前有 0 和 1 两个桌面，其中 0 为 taskbar 中的 toogleDesktopButton 开关所代表的 “临时桌面”。
    /// 目前，点击 toogleDesktopButton 的效果就是将目前打开的窗口都收纳入“临时桌面”；
    /// 如果“临时桌面”中存在暂存的窗口，那么此时点击“临时桌面”，这些暂存窗口将恢复到“当前桌面”。
    ///
    /// 未来会实现将窗口拖拽到“临时桌面”中，这样可以实现在多个桌面中移动窗口
    ///
    /// 默认是 1
    /// </summary>
    [JsonPropertyName("desktopIndex")]
    public int DesktopIndex { get => desktopIndex.Get(); set => desktopIndex.Set(value); }
    private Observable.Observer desktopIndex { get; init; }

    /// <summary>
    /// 当前窗口所在的屏幕
    ///
    /// > 配合 getScreens 接口，就能获得当前屏幕的详细信息。参考 [`Electron.screen.getAllDisplays(): Electron.Display[]`](https://electronjs.org/docs/api/structures/display)
    /// > 未来实现多设备互联时，可以实现窗口的多设备流转
    /// > 屏幕与桌面是两个独立的概念
    ///
    /// 默认是 -1，意味着使用“主桌面”
    /// </summary>
    [JsonPropertyName("screenId")]
    public int ScreenId { get => screenId.Get(); set => screenId.Set(value); }
    private Observable.Observer screenId { get; init; }

    /// <summary>
    /// 内容渲染是否要覆盖 顶部栏
    /// </summary>
    [JsonPropertyName("topBarOverlay")]
    public bool TopBarOverlay { get => topBarOverlay.Get(); set => topBarOverlay.Set(value); }
    private Observable.Observer topBarOverlay { get; init; }

    /// <summary>
    /// 内容渲染是否要覆盖 底部栏
    /// </summary>
    [JsonPropertyName("bottomBarOverlay")]
    public bool BottomBarOverlay { get => bottomBarOverlay.Get(); set => bottomBarOverlay.Set(value); }
    private Observable.Observer bottomBarOverlay { get; init; }

    /// <summary>
    /// 应用的主题色，格式为 #RRGGBB ｜ auto
    ///
    /// 如果使用 auto，则会根据当前的系统的显示模式，自动跟随成 亮色 或者 暗色
    /// </summary>
    [JsonPropertyName("themeColor")]
    public string ThemeColor { get => themeColor.Get(); set => themeColor.Set(value); }
    private Observable.Observer themeColor { get; init; }

    /// <summary>
    /// 顶部栏的文字颜色，格式为 #RRGGBB | auto
    ///
    /// 如果使用 auto，会自动根据现有的背景色来显示 亮色 或者 暗色
    /// </summary>
    [JsonPropertyName("topBarContentColor")]
    public string TopBarContentColor { get => topBarContentColor.Get(); set => topBarContentColor.Set(value); }
    private Observable.Observer topBarContentColor { get; init; }

    /// <summary>
    /// 顶部栏的文字颜色，格式为 #RRGGBB ｜ auto
    ///
    /// 如果使用 auto，会与 themeColor 保持一致
    /// </summary>
    [JsonPropertyName("topBarBackgroundColor")]
    public string TopBarBackgroundColor { get => topBarBackgroundColor.Get(); set => topBarBackgroundColor.Set(value); }
    private Observable.Observer topBarBackgroundColor { get; init; }

    /// <summary>
    /// 底部栏的文字颜色，格式为 #RRGGBB | auto
    ///
    /// 如果使用 auto，会自动根据现有的背景色来显示 亮色 或者 暗色
    /// </summary>
    [JsonPropertyName("bottomBarContentColor")]
    public string BottomBarContentColor { get => bottomBarContentColor.Get(); set => bottomBarContentColor.Set(value); }
    private Observable.Observer bottomBarContentColor { get; init; }

    /// <summary>
    /// 底部栏的文字颜色，格式为 #RRGGBB ｜ auto
    ///
    /// 如果使用 auto，会与 themeColor 保持一致
    /// </summary>
    [JsonPropertyName("bottomBarBackgroundColor")]
    public string BottomBarBackgroundColor { get => bottomBarBackgroundColor.Get(); set => bottomBarBackgroundColor.Set(value); }
    private Observable.Observer bottomBarBackgroundColor { get; init; }

    /// <summary>
    /// 底部栏的风格，默认是导航模式
    /// </summary>
    [JsonPropertyName("bottomBarTheme")]
    public WindowBottomBarTheme BottomBarTheme { get => bottomBarTheme.Get(); set => bottomBarTheme.Set(value); }
    private Observable.Observer bottomBarTheme { get; init; }

    /// <summary>
    /// 窗口关闭的提示信息
    ///
    /// 如果非 null（即便是空字符串），那么窗口关闭前，会提供提示信息
    /// </summary>
    [JsonPropertyName("closeTip")]
    public string? CloseTip { get => closeTip.Get(); set => closeTip.Set(value); }
    private Observable.Observer closeTip { get; init; }

    /// <summary>
    /// 是否在显示窗口提示信息
    ///
    /// PS：开发者可以监听这个属性，然后动态地去修改 closeTip。如果要禁用这种行为，可以将 showCloseTip 的类型修改成 String?
    /// </summary>
    [JsonPropertyName("showCloseTip")]
    public bool ShowCloseTip { get => showCloseTip.Get(); set => showCloseTip.Set(value); }
    private Observable.Observer showCloseTip { get; init; }

    #endregion

    [Obsolete("使用带参数的构造函数", true)]
#pragma warning disable CS8618 // 在退出构造函数时，不可为 null 的字段必须包含非 null 值。请考虑声明为可以为 null。
    public WindowState()
#pragma warning restore CS8618 // 在退出构造函数时，不可为 null 的字段必须包含非 null 值。请考虑声明为可以为 null。
    {
        /// 给JSON反序列化用的空参数构造函数
    }

    public WindowState(Mmid owner, Mmid provider, UUID? wid = null, MicroModule? microModule = null)
    {
        Wid = wid ?? Guid.NewGuid().ToString();
        Owner = owner;
        Provider = provider;
        MicroModule = microModule;

        bounds = Observable.Observe(WindowPropertyKeys.Bounds.FieldName, new WindowBounds());
        title = Observable.ObserveNullable<string>(WindowPropertyKeys.Title.FieldName);
        //iconUrl = Observable.ObserveNullable<string>(WindowPropertyKeys.IconUrl.FieldName);
        iconUrl = Observable.Observe(WindowPropertyKeys.IconUrl.FieldName, string.Empty);
        iconMaskable = Observable.Observe(WindowPropertyKeys.IconMaskable.FieldName, false);
        iconMonochrome = Observable.Observe(WindowPropertyKeys.IconMonochrome.FieldName, false);
        mode = Observable.Observe(WindowPropertyKeys.Mode.FieldName, WindowMode.FLOATING);
        canGoBack = Observable.ObserveNullable<bool>(WindowPropertyKeys.CanGoBack.FieldName, false);
        canGoForward = Observable.ObserveNullable<bool>(WindowPropertyKeys.CanGoForward.FieldName, null);
        resizable = Observable.Observe(WindowPropertyKeys.Resizable.FieldName, false);
        focus = Observable.Observe(WindowPropertyKeys.Focus.FieldName, false);
        zIndex = Observable.Observe(WindowPropertyKeys.ZIndex.FieldName, 0);
        children = Observable.Observe(WindowPropertyKeys.Children.FieldName, new List<UUID>());
        parent = Observable.ObserveNullable<UUID>(WindowPropertyKeys.Parent.FieldName);
        flashing = Observable.Observe(WindowPropertyKeys.Flashing.FieldName, false);
        flashColor = Observable.Observe(WindowPropertyKeys.FlashColor.FieldName, UIColor.White.ToHex());
        progressBar = Observable.Observe(WindowPropertyKeys.ProgressBar.FieldName, -1f);
        alwaysOnTop = Observable.Observe(WindowPropertyKeys.AlwaysOnTop.FieldName, false);
        desktopIndex = Observable.Observe(WindowPropertyKeys.DesktopIndex.FieldName, 1);
        screenId = Observable.Observe(WindowPropertyKeys.ScreenId.FieldName, -1);
        topBarOverlay = Observable.Observe(WindowPropertyKeys.TopBarOverlay.FieldName, false);
        bottomBarOverlay = Observable.Observe(WindowPropertyKeys.BottomBarOverlay.FieldName, false);
        themeColor = Observable.Observe(WindowPropertyKeys.ThemeColor.FieldName, "auto");
        topBarContentColor = Observable.Observe(WindowPropertyKeys.TopBarContentColor.FieldName, "auto");
        topBarBackgroundColor = Observable.Observe(WindowPropertyKeys.TopBarBackgroundColor.FieldName, "auto");
        bottomBarContentColor = Observable.Observe(WindowPropertyKeys.BottomBarContentColor.FieldName, "auto");
        bottomBarBackgroundColor = Observable.Observe(WindowPropertyKeys.BottomBarBackgroundColor.FieldName, "auto");
        bottomBarTheme = Observable.Observe(WindowPropertyKeys.BottomBarTheme.FieldName, WindowBottomBarTheme.Navigation);
        closeTip = Observable.ObserveNullable<string>(WindowPropertyKeys.CloseTip.FieldName, null);
        showCloseTip = Observable.Observe(WindowPropertyKeys.ShowCloseTip.FieldName, false);
    }

    public WindowState ToJsonAble() => this;
}
