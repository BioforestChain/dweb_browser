using System.Text.Json;
using System.Text.Json.Serialization;

namespace DwebBrowser.MicroService.Core;

public class WindowPropertyKeys
{
    public static readonly WindowPropertyKeys Any = new("*");
    public static readonly WindowPropertyKeys Title = new("title");
    public static readonly WindowPropertyKeys IconUrl = new("iconUrl");
    public static readonly WindowPropertyKeys Mode = new("mode");
    public static readonly WindowPropertyKeys Resizable = new("resizable");
    public static readonly WindowPropertyKeys Focus = new("focus");
    public static readonly WindowPropertyKeys PictureInPicture = new("pictureInPicture");
    public static readonly WindowPropertyKeys ZIndex = new("zIndex");
    public static readonly WindowPropertyKeys Children = new("children");
    public static readonly WindowPropertyKeys Parent = new("parent");
    public static readonly WindowPropertyKeys Flashing = new("flashing");
    public static readonly WindowPropertyKeys FlashColor = new("flashColor");
    public static readonly WindowPropertyKeys ProgressBar = new("progressBar");
    public static readonly WindowPropertyKeys AlwaysOnTop = new("alwaysOnTop");
    public static readonly WindowPropertyKeys DesktopIndex = new("desktopIndex");
    public static readonly WindowPropertyKeys ScreenId = new("screenId");
    public static readonly WindowPropertyKeys TopBarOverlay = new("topBarOverlay");
    public static readonly WindowPropertyKeys BottomBarOverlay = new("bottomBarOverlay");
    public static readonly WindowPropertyKeys TopBarContentColor = new("topBarContentColor");
    public static readonly WindowPropertyKeys TopBarBackgroundColor = new("topBarBackgroundColor");
    public static readonly WindowPropertyKeys BottomBarContentColor = new("bottomBarContentColor");
    public static readonly WindowPropertyKeys BottomBarBackgroundColor = new("bottomBarBackgroundColor");
    public static readonly WindowPropertyKeys BottomBarTheme = new("bottomBarTheme");
    public static readonly WindowPropertyKeys ThemeColor = new("themeColor");
    public static readonly WindowPropertyKeys Bounds = new("bounds");

    public string FieldName { get; init; }
    public WindowPropertyKeys(string fieldName)
    {
        FieldName = fieldName;
    }
}

#region WindowMode Enum Type
#region WindowMode
[JsonConverter(typeof(WindowModeConverter))]
public class WindowMode
{
    [JsonPropertyName("mode")]
    public string Mode { get; init; }

    public WindowMode(string mode)
    {
        Mode = mode;
    }

    /// <summary>
    /// 浮动模式，默认值
    /// </summary>
    public static readonly WindowMode FLOATING = new("floating");

    /// <summary>
    /// 最大化
    /// </summary>
    public static readonly WindowMode MAXIMIZE = new("maximize");

    /// <summary>
    /// 最小化
    /// </summary>
    public static readonly WindowMode MINIMIZE = new("minimize");

    /// <summary>
    /// 全屏
    /// </summary>
    public static readonly WindowMode FULLSCREEN = new("fullscreen");

    /// <summary>
    /// 画中画模式
    ///    * 与原生的 PIP 不同,原生的PIP有一些限制,比如 IOS 上只能渲染 Media。
    /// 而在 desk 中的 PIP 原理简单粗暴,就是将视图进行 clip+scale,因此它本质上还是渲染一整个 win-view。
    /// 并且此时这个被裁切的窗口将无法接收到任何用户的手势、键盘等输入,却而代之的,接口中允许一些自定义1~4个的 icon-button,这些按钮将会被渲染在 pip-controls-bar (PIP 的下方) 中方便用户操控当前的 PIP。
    ///    * 多个 PIP 视图会被叠在一起,在一个 max-width == max-height 的空间中,所有 PIP 以 contain 的显示形式居中放置。
    ///    只会有一个 PIP 视图显示在最前端,其它层叠显示在它后面
    /// PIP 可以全局拖动。
    /// PIP 会有两个状态:聚焦和失焦。
    ///    点击后,进入聚焦模式,视图轻微放大,pip-controlls-bar 会从 PIP 视图的 Bottom 与 End 侧 显示出来;
    ///        其中 Bottom 侧显示的是用户自定义的 icon-button,以 space-around 的显示形式水平放置;
    ///        同时 End 侧显示的是 PIP 的 dot-scroll-bar(应该是拟物设计,否则用户认知成本可能会不低),桌面端可以点击或者滚轮滚动、移动端可以上下滑动,从而切换最前端的 PIP 视图
    ///        聚焦模式下 PIP 仍然可以全局拖动,但是一旦造成拖动,会同时切换成失焦模式。
    ///    在聚焦模式下,再次点击 PIP,将会切换到失焦模式,此时 pip-controlls-bar 隐藏,视图轻微缩小;
    /// PIP 的视图的 End-Corner 是一个 resize 区域,用户可以随意拖动这个来对视图进行resize,同时开发者会收到resize指令,从而作出“比例响应式”变更。如果开发者不响应该resize,那么 PIP 会保留 win-view 的渲染比例。
    ///    * > 注意:该模式下,alwaysOnTop 为 true,并且将不再显示 win-controls-bar。
    /// > 提示:如果不想 PIP 功能把当前的 win-view  吃掉,那么可以打开一个子窗口来申请 PIP 模式。
    /// </summary>
    public static readonly WindowMode PIP = new("picture-in-picture");

    /// <summary>
    /// 窗口关闭
    /// </summary>
    public static readonly WindowMode CLOSED = new("closed");

    /// <summary>
    /// Serialize WindowMode
    /// </summary>
    /// <returns>JSON string representation of the WindowMode</returns>
    public string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize WindowMode
    /// </summary>
    /// <param name="json">JSON string representation of WindowMode</param>
    /// <returns>An instance of a WindowMode object.</returns>
    public static WindowMode? FromJson(string json) => JsonSerializer.Deserialize<WindowMode>(json);
}
#endregion

#region WindowMode序列化反序列化
public class WindowModeConverter : JsonConverter<WindowMode>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") is not null && typeToConvert.GetMethod("FromJson") is not null;

    public override WindowMode? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        var mode = reader.GetString();
        return mode switch
        {
            "floating" => WindowMode.FLOATING,
            "maximize" => WindowMode.MAXIMIZE,
            "minimize" => WindowMode.MINIMIZE,
            "fullscreen" => WindowMode.FULLSCREEN,
            "picture-in-picture" => WindowMode.PIP,
            "closed" => WindowMode.CLOSED,
            _ => throw new JsonException("Invalid WindowMode Type")
        };

    }

    public override void Write(Utf8JsonWriter writer, WindowMode value, JsonSerializerOptions options)
    {
        writer.WriteStringValue(value.Mode);
    }
}
#endregion

#endregion

//  SPLIT_SCREEN, // 分屏模式
//  SNAP_LEFT, // 屏幕左侧对齐
//  SNAP_RIGHT, // 屏幕右侧对齐
//  CASCADE, // 级联模式
//  TILE_HORIZONTALLY, // 水平平铺
//  TILE_VERTICALLY, // 垂直平铺
//  FLOATING, // 浮动模式
//  PIP, // 画中画模式
//
//  CUSTOM // 自定义模式


#region WindowBottomBarTheme Enum Type
#region WindowBottomBarTheme
[JsonConverter(typeof(WindowBottomBarThemeConverter))]
public class WindowBottomBarTheme
{
    [JsonPropertyName("themeName")]
    public string ThemeName { get; init; }

    public WindowBottomBarTheme(string themeName)
    {
        ThemeName = themeName;
    }

    public static WindowBottomBarTheme From(string themeName) => themeName is "navigation" or "immersion" ? new(themeName) : Navigation;

    /// <summary>
    /// 导航模式：较高,面向常见的网页,依次提供app-id+version(两行小字显示)、back-bottom、forword-bottom、unmax bottom(1)。
    /// 点击app-id等效于点击顶部的titlebar展开的菜单栏(显示窗窗口信息、所属应用信息、一些设置功能(比如刷新页面、设置分辨率、设置UA、查看路径))
    /// </summary>
    public static readonly WindowBottomBarTheme Navigation = new("navigation");

    /// <summary>
    /// 沉浸模式：较矮,只提供app-id+version的信息(一行小字)
    /// </summary>
    public static readonly WindowBottomBarTheme Immersion = new("immersion");

    /// <summary>
    /// Serialize WindowBottomBarTheme
    /// </summary>
    /// <returns>JSON string representation of the WindowBottomBarTheme</returns>
    public string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize WindowBottomBarTheme
    /// </summary>
    /// <param name="json">JSON string representation of WindowBottomBarTheme</param>
    /// <returns>An instance of a WindowBottomBarTheme object.</returns>
    public static WindowBottomBarTheme? FromJson(string json) => JsonSerializer.Deserialize<WindowBottomBarTheme>(json);
}

#endregion

#region WindowBottomBarTheme序列化反序列化
public class WindowBottomBarThemeConverter : JsonConverter<WindowBottomBarTheme>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") is not null && typeToConvert.GetMethod("FromJson") is not null;

    public override WindowBottomBarTheme? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        var themeName = reader.GetString();
        return themeName switch
        {
            "navigation" => WindowBottomBarTheme.Navigation,
            "immersion" => WindowBottomBarTheme.Immersion,
            _ => throw new JsonException("Invalid WindowBottomBarTheme Type")
        };

    }

    public override void Write(Utf8JsonWriter writer, WindowBottomBarTheme value, JsonSerializerOptions options)
    {
        writer.WriteStringValue(value.ThemeName);
    }
}
#endregion

#endregion

