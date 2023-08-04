using System.Text.Json.Serialization;
using UIKit;

namespace DwebBrowser.MicroService.Core;

using CreateWindowAdapter = Func<WindowState, Task<WindowController>>;

public class WindowAdapterManager : AdapterManager<CreateWindowAdapter>
{
    public static readonly WindowAdapterManager WindowAdapterManagerInstance = new();

    public Task<WindowController> CreateWindow(WindowState winState)
    {
        foreach (var adapter in Adapters)
        {
            var winCtrl = adapter(winState);
            if (winCtrl is not null)
            {
                return winCtrl;
            }
        }

        throw new Exception($"no support create native window, owner: {winState.Owner} provider: {winState.Provider}");
    }
}

public abstract class WindowController
{
    /// <summary>
    /// 在iOS中，一个UIView附着在某一个UIViewController中
    /// </summary>
    public abstract UIViewController Controller { get; init; }

    public abstract WindowState ToJson();

    protected readonly HashSet<Signal> _destroySignal = new();
    public event Signal OnDestroy
    {
        add { if (value != null) lock (_destroySignal) { _destroySignal.Add(value); } }
        remove { lock (_destroySignal) { _destroySignal.Remove(value); } }
    }
    private bool IsDestroy = false;
    public bool IsDestroyed() => IsDestroy;

    public Task Close(bool force = false)
    {
        /// 这里的 force 暂时没有作用，未来会加入交互，来阻止窗口关闭

        IsDestroy = true;
        return _destroySignal.EmitAndClear();
    }
}



/// <summary>
/// 单个窗口的状态集
/// </summary>
public class WindowState
{
    /// <summary>
    /// 窗口全局唯一编号，属于UUID的格式
    /// </summary>
    [JsonPropertyName("wid")]
    public UUID Wid { get; set; }

    /// <summary>
    /// 窗口持有者
    ///
    /// 窗口创建者
    /// </summary>
    [JsonPropertyName("owner")]
    public Mmid Owner { get; set; }

    /// <summary>
    /// 内容提提供方
    ///
    /// 比如若渲染的是web内容，那么应该是 mwebview.browser.dweb
    /// </summary>
    [JsonPropertyName("provider")]
    public Mmid Provider { get; set; }

    /// <summary>
    /// 窗口标题
    ///
    /// 该标题不需要一定与应用名称相同
    ///
    /// 如果是 mwebview，默认会采用当前 Webview 的网页 title
    /// </summary>
    [JsonPropertyName("title")]
    public string? Title { get; set; }

    /// <summary>
    /// 应用图标链接
    ///
    /// 该链接与应用图标不同
    ///
    /// 如果是 mwebview，默认会采用当前 Webview 的网页 favicon
    /// </summary>
    [JsonPropertyName("iconUrl")]
    public string? IconUrl { get; set; }

    /// <summary>
    /// 窗口位置和大小
    ///
    /// 窗口会被限制最小值，会被限制显示区域。
    /// 终止，窗口最终会被绘制在用户可见可控的区域中
    /// </summary>
    [JsonPropertyName("bounds")]
    public WindowBounds? Bounds { get; set; }

    /// <summary>
    /// 是否全屏
    /// </summary>
    [JsonPropertyName("fullscreen")]
    public bool Fullscreen { get; set; }

    /// <summary>
    /// 是否最大化，如果全屏状态，那么该值也会同时为 true
    /// </summary>
    [JsonPropertyName("maximize")]
    public bool Maximize { get; set; }

    /// <summary>
    /// 是否最小化
    /// </summary>
    [JsonPropertyName("minimize")]
    public bool Minimize { get; set; }

    /// <summary>
    /// 当前是否缩放窗口
    /// </summary>
    [JsonPropertyName("resizable")]
    public bool Resizable { get; set; }

    /// <summary>
    /// 是否聚焦
    ///
    /// 目前只会有一个窗口被聚焦，未来实现多窗口联合显示的时候，就可能会有多个窗口同时focus，但这取决于所处宿主操作系统的支持。
    /// </summary>
    [JsonPropertyName("focus")]
    public bool Focus { get; set; }

    /// <summary>
    /// 是否处于画中画模式
    ///
    /// 与原生的 PIP 不同，原生的PIP有一些限制，比如 IOS 上只能渲染 Media。
    /// 而在 desk 中的 PIP 原理简单粗暴，就是将视图进行 clip+scale，因此它本质上还是渲染一整个 win-view。
    /// 并且此时这个被裁切的窗口将无法接收到任何用户的手势、键盘等输入，却而代之的，接口中允许一些自定义1～4个的 icon-button，这些按钮将会被渲染在 pip-controls-bar （PIP 的下方） 中方便用户操控当前的 PIP。
    ///
    /// 多个 PIP 视图会被叠在一起，在一个 max-width == max-height 的空间中，所有 PIP 以 contain 的显示形式居中放置。
    ///    只会有一个 PIP 视图显示在最前端，其它层叠显示在它后面
    /// PIP 可以全局拖动。
    /// PIP 会有两个状态：聚焦和失焦。
    ///    点击后，进入聚焦模式，视图轻微放大，pip-controlls-bar 会从 PIP 视图的 Bottom 与 End 侧 显示出来；
    ///        其中 Bottom 侧显示的是用户自定义的 icon-button，以 space-around 的显示形式水平放置；
    ///        同时 End 侧显示的是 PIP 的 dot-scroll-bar（应该是拟物设计，否则用户认知成本可能会不低），桌面端可以点击或者滚轮滚动、移动端可以上下滑动，从而切换最前端的 PIP 视图
    ///        聚焦模式下 PIP 仍然可以全局拖动，但是一旦造成拖动，会同时切换成失焦模式。
    ///    在聚焦模式下，再次点击 PIP，将会切换到失焦模式，此时 pip-controlls-bar 隐藏，视图轻微缩小；
    /// PIP 的视图的 End-Corner 是一个 resize 区域，用户可以随意拖动这个来对视图进行resize，同时开发者会收到resize指令，从而作出“比例响应式”变更。如果开发者不响应该resize，那么 PIP 会保留 win-view 的渲染比例。
    ///
    /// > 注意：该模式下，alwaysOnTop 为 true，并且将不再显示 win-controls-bar。
    /// > 提示：如果不想 PIP 功能把当前的 win-view  吃掉，那么可以打开一个子窗口来申请 PIP 模式。
    /// </summary>
    [JsonPropertyName("pictureInPicture")]
    public bool PictureInPicture { get; set; }

    /// <summary>
    /// 当前窗口层叠顺序
    /// @types {float} 这里使用float，本质上意味着这个zIndex是一个“二维”值
    ///
    /// 通常默认情况下都是整数。
    /// 如果一个窗口有了子窗口，那么这个 父窗口 和 这些 子窗口 会使用小数来区分彼此；同时父窗口总是为整数
    /// 这里没有将它拆分成两个数来存储，目的是复合直觉
    /// </summary>
    [JsonPropertyName("zIndex")]
    public int ZIndex { get; set; }

    /// <summary>
    /// 子窗口
    /// </summary>
    [JsonPropertyName("children")]
    public List<UUID> Children { get; set; }

    /// <summary>
    /// 父窗口
    /// </summary>
    [JsonPropertyName("parent")]
    public UUID? Parent { get; set; }

    /// <summary>
    /// 是否在闪烁提醒
    /// </summary>
    [JsonPropertyName("flashing")]
    public bool Flashing { get; set; }

    /// <summary>
    /// > 类似 macos 中的图标弹跳、windows 系统中的窗口闪烁。
    /// 在 taskbar 中， running-dot 会闪烁变色
    /// </summary>
    [JsonPropertyName("flashColor")]
    public string? FlashColor { get; set; }

    /// <summary>
    /// 进度条
    /// 
    /// 范围为 `[0～1]`
    /// 如果小于0（通常为 -1），那么代表没有进度条信息，否则将会在taskbar中显示它的进度信息
    /// </summary>
    [JsonPropertyName("progressBar")]
    public float ProgressBar { get; set; }

    /// <summary>
    /// 是否置顶显示
    ///
    /// 这与 zIndex 不冲突，置顶只是一个优先渲染的层级，可以简单理解成 `zIndex+1000`
    ///
    /// > 前期我们应该不会在移动设备上开放这个接口，因为移动设备的可用空间非常有限，如果允许任意窗口置顶，那么用户体验将会非常糟。
    /// > 如果需要置顶功能，可以考虑使用 pictureInPicture
    /// </summary>
    [JsonPropertyName("alwaysOnTop")]
    public bool AlwaysOnTop { get; set; }

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
    public int DesktopIndex { get; set; }

    /// <summary>
    /// 当前窗口所在的屏幕
    ///
    /// > 配合 getScreens 接口，就能获得当前屏幕的详细信息。参考 [`Electron.screen.getAllDisplays(): Electron.Display[]`](https://electronjs.org/docs/api/structures/display)
    /// > 未来实现多设备互联时，可以实现窗口的多设备流转
    /// > 屏幕与桌面是两个独立的概念
    ///
    /// 默认是 -1，意味着使用“主桌面”
    /// </summary>
    [JsonPropertyName("screenIndex")]
    public int ScreenIndex { get; set; }

    [Obsolete("使用带参数的构造函数", true)]
#pragma warning disable CS8618 // 在退出构造函数时，不可为 null 的字段必须包含非 null 值。请考虑声明为可以为 null。
    public WindowState()
#pragma warning restore CS8618 // 在退出构造函数时，不可为 null 的字段必须包含非 null 值。请考虑声明为可以为 null。
    {
        /// 给JSON反序列化用的空参数构造函数
    }

    public WindowState(
          UUID wid,
          Mmid owner,
          Mmid provider,
          string? title = null,
          WindowBounds? bounds = null,
          bool fullscreen = false,
          bool maximize = false,
          bool minimize = false,
          bool resizable = false,
          bool focus = false,
          bool pictureInPicture = false,
          int zIndex = 0,
          List<UUID>? children = null,
          UUID? parent = null,
          bool flashing = false,
          string? flashColor = null,
          float progressBar = -1f,
          bool alwaysOnTop = false,
          int desktopIndex = -1,
          int screenIndex = -1)
    {
        title ??= owner;
        bounds ??= new();
        children ??= new();
        flashColor ??= UIColor.White.ToCssRgba();
        Wid = wid;
        Owner = owner;
        Provider = provider;
        Title = title;
        Bounds = bounds;
        Fullscreen = fullscreen;
        Maximize = maximize;
        Minimize = minimize;
        Resizable = resizable;
        Focus = focus;
        PictureInPicture = pictureInPicture;
        ZIndex = zIndex;
        Children = children;
        Parent = parent;
        Flashing = flashing;
        FlashColor = flashColor;
        ProgressBar = progressBar;
        AlwaysOnTop = alwaysOnTop;
        DesktopIndex = desktopIndex;
        ScreenIndex = screenIndex;
    }

    /// <summary>
    /// 窗口大小与位置
    ///
    /// 默认值是NaN，这种情况下，窗口构建者需要自己对其进行赋值
    /// </summary>
    /// <param name="left"></param>
    /// <param name="top"></param>
    /// <param name="width"></param>
    /// <param name="height"></param>
    public class WindowBounds
    {
        [JsonPropertyName("left")]
        public float Left { get; set; }

        [JsonPropertyName("top")]
        public float Top { get; set; }

        [JsonPropertyName("width")]
        public float Width { get; set; }

        [JsonPropertyName("height")]
        public float Height { get; set; }

        public WindowBounds(float left = float.NaN, float top = float.NaN, float width = float.NaN, float height = float.NaN)
        {
            Left = left;
            Top = top;
            Width = width;
            Height = height;
        }
    }

    protected readonly HashSet<Signal> ChangeSignal = new();
    public event Signal OnChange
    {
        add { if (value != null) lock (ChangeSignal) { ChangeSignal.Add(value); } }
        remove { lock (ChangeSignal) { ChangeSignal.Remove(value); } }
    }

    public Task EmitChange() => ChangeSignal.Emit();
}


