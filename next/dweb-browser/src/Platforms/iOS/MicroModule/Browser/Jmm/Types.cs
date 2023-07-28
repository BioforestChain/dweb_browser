using System.Text.Json.Serialization;

namespace DwebBrowser.MicroService.Browser.Jmm;

/// <summary>
/// js 应用程序的入口
/// </summary>
public struct MainServer
{
    /// <summary>
    /// root 定义程序的启动目录
    /// </summary>
    [JsonPropertyName("root")]
    public string Root { get; set; }

    /// <summary>
    /// root 定义程序的启动文件
    /// </summary>
    [JsonPropertyName("entry")]
    public string Entry { get; set; }
}

public interface IJmmAppManifest : ICommonAppManifest
{
    public new Mmid Id { get; set; }
    /// <summary>
    /// 版本信息
    /// </summary>
    public new string Version { get; set; }
    /// <summary>
    /// 类目
    /// </summary>
    public new List<MicroModuleCategory> Categories { get; set; }
    /// <summary>
    /// js 应用程序的入口
    /// </summary>
    public MainServer Server { get; set; }
    /// <summary>
    /// 基准URL，如果没有定义了这个url，那么默认使用当前的 .json 链接
    /// </summary>
    public string? BaseUri { get; set; }
    public List<Dweb_DeepLink>? Dweb_DeepLinks { get; set; }

    public new TextDirectionType Dir { get; set; }
    public new string Lang { get; set; }
    public new string Name { get; set; }
    public new string ShortName { get; set; }
    public new string Description { get; set; }
    public new List<Core.ImageSource> Icons { get; set; }
    public new List<Core.ImageSource> Screenshots { get; set; }
    public new DisplayModeType Display { get; set; }
    public new OrientationType Orientation { get; set; }
    public new string ThemeColor { get; set; }
    public new string BackgroundColor { get; set; }
    public new List<ShortcutItem> Shortcuts { get; set; }
}

public interface IJmmAppInstallManifest : IJmmAppManifest
{
    /// <summary>
    /// 安装是展示用的 icon
    /// </summary>
    public string Icon { get; set; }
    /// <summary>
    /// 安装时展示用的截图
    /// </summary>
    public List<string> Images { get; set; }
    public string BundleUrl { get; set; }
    public string BundleHash { get; set; }
    public long BundleSize { get; set; }
    /// <summary>
    /// 更新日志，直接放url
    /// </summary>
    public string ChangeLog { get; set; }
    /// <summary>
    /// 安装时展示的作者信息
    /// </summary>
    public List<string> Author { get; set; }
    /// <summary>
    /// 安装时展示的主页链接
    /// </summary>
    public string Home { get; set; }
    /// <summary>
    /// 安装时展示的发布日期
    /// </summary>
    public string ReleaseDate { get; set; }
    /// <summary>
    /// @deprecated 安装时显示的权限信息
    /// </summary>
    public List<string> Permissions { get; set; }
    /// <summary>
    /// @deprecated 安装时显示的依赖模块
    /// </summary>
    public List<string> Plugins { get; set; }
}

