using System.Text.Json;
using System.Text.Json.Serialization;

#nullable enable

namespace DwebBrowser.MicroService.Browser.Jmm;

public class JmmAppInstallManifest : IJmmAppInstallManifest
{
    [JsonPropertyName("id")]
    public Mmid Id { get; set; }
    [JsonPropertyName("version")]
    public string Version { get; set; }
    [JsonPropertyName("categories")]
    public List<MicroModuleCategory> Categories { get; set; }
    [JsonPropertyName("server")]
    public MainServer Server { get; set; }
    [JsonPropertyName("baseURI")]
    public string? BaseUri { get; set; }
    [JsonPropertyName("dweb_deeplinks")]
    public List<string>? Dweb_DeepLinks { get; set; }
    [JsonPropertyName("dir")]
    public TextDirectionType Dir { get; set; }
    [JsonPropertyName("lang")]
    public string Lang { get; set; }
    [JsonPropertyName("name")]
    public string Name { get; set; }
    [JsonPropertyName("short_name")]
    public string ShortName { get; set; }
    [JsonPropertyName("description")]
    public string Description { get; set; }
    [JsonPropertyName("icons")]
    public List<Core.ImageSource> Icons { get; set; }
    [JsonPropertyName("screenshots")]
    public List<Core.ImageSource> Screenshots { get; set; }
    [JsonPropertyName("display")]
    public DisplayModeType Display { get; set; }
    [JsonPropertyName("orientation")]
    public OrientationType Orientation { get; set; }
    [JsonPropertyName("theme_color")]
    public string ThemeColor { get; set; }
    [JsonPropertyName("background_color")]
    public string BackgroundColor { get; set; }
    [JsonPropertyName("shortcuts")]
    public List<ShortcutItem> Shortcuts { get; set; }
    [JsonPropertyName("icon")]
    public string Icon { get; set; }
    [JsonPropertyName("images")]
    public List<string> Images { get; set; }
    [JsonPropertyName("bundle_url")]
    public string BundleUrl { get; set; }
    [JsonPropertyName("bundle_hash")]
    public string BundleHash { get; set; }
    [JsonPropertyName("bundle_size")]
    public long BundleSize { get; set; }
    [JsonPropertyName("change_log")]
    public string ChangeLog { get; set; }
    [JsonPropertyName("author")]
    public List<string> Author { get; set; }
    [JsonPropertyName("home")]
    public string Home { get; set; }
    [JsonPropertyName("release_date")]
    public string ReleaseDate { get; set; }
    [JsonPropertyName("permissions")]
    public List<string> Permissions { get; set; }
    [JsonPropertyName("plugins")]
    public List<string> Plugins { get; set; }

    [Obsolete("使用带参数的构造函数", true)]
#pragma warning disable CS8618 // 在退出构造函数时，不可为 null 的字段必须包含非 null 值。请考虑声明为可以为 null。
    public JmmAppInstallManifest()
#pragma warning restore CS8618 // 在退出构造函数时，不可为 null 的字段必须包含非 null 值。请考虑声明为可以为 null。
    {
        /// 给JSON反序列化用的空参数构造函数
    }

    public JmmAppInstallManifest(
        Mmid mmid,
        string version,
        List<MicroModuleCategory> categories,
        MainServer server,
        TextDirectionType dir,
        string lang,
        string name,
        string short_name,
        string description,
        List<Core.ImageSource> icons,
        List<Core.ImageSource> screenshots,
        DisplayModeType display,
        OrientationType orientation,
        string theme_color,
        string background_color,
        List<ShortcutItem> shortcuts,
        string icon,
        List<string> images,
        string bundle_url,
        string bundle_hash,
        long bundle_size,
        string change_log,
        List<string> author,
        string home,
        string release_date,
        List<string> permissions,
        List<string> plugins,
        string? baseURI = null,
        List<string>? dweb_deeplinks = null)
    {
        Id = mmid;
        Server = server;
        Dir = dir;
        Lang = lang;
        Icons = icons;
        Screenshots = screenshots;
        Display = display;
        Orientation = orientation;
        ThemeColor = theme_color;
        BackgroundColor = background_color;
        Shortcuts = shortcuts;
        ChangeLog = change_log;
        BaseUri = baseURI;
        Name = name;
        ShortName = short_name;
        Icon = icon;
        Images = images;
        Description = description;
        Author = author;
        Version = version;
        Categories = categories;
        Home = home;
        BundleUrl = bundle_url;
        BundleSize = bundle_size;
        BundleHash = bundle_hash;
        Permissions = permissions;
        Plugins = plugins;
        ReleaseDate = release_date;
        if (dweb_deeplinks is null)
        {
            Dweb_DeepLinks = new();
        }
        else
        {
            Dweb_DeepLinks = dweb_deeplinks;
        }
    }

    public virtual string ToJson() => JsonSerializer.Serialize(this);
    public static JmmAppInstallManifest? FromJson(string json) =>
        JsonSerializer.Deserialize<JmmAppInstallManifest>(json);
}
