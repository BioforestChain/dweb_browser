using System.Text.Json;
using System.Text.Json.Serialization;

#nullable enable

namespace DwebBrowser.MicroService.Browser.Jmm;

public class JmmMetadata
{
    [JsonPropertyName("id")]
    public Mmid Id { get; set; }    // jmmApp的id
    [JsonPropertyName("server")]
    public MainServer Server { get; set; }      // 打开应用地址
    [JsonPropertyName("title")]
    public string Title { get; set; }       // 应用名称
    [JsonPropertyName("shortName")]
    public string ShortName { get; set; }        // 应用副标题
    [JsonPropertyName("icon")]
    public string Icon { get; set; }        // 应用图标
    [JsonPropertyName("downloadUrl")]
    public string DownloadUrl { get; set; }     // 下载应用地址
    [JsonPropertyName("images")]
    public List<string>? Images { get; set; }       // 应用截图
    [JsonPropertyName("description")]
    public string Description { get; set; }        // 应用描述
    [JsonPropertyName("author")]
    public List<string>? Author { get; set; }       // 开发者，作者
    [JsonPropertyName("version")]
    public string Version { get; set; }     // 应用版本
    [JsonPropertyName("newFeature")]
    public string NewFeature { get; set; }      // 新特性，新功能
    [JsonPropertyName("keywords")]
    public List<string>? Keywords { get; set; }     // 关键词
    [JsonPropertyName("home")]
    public string Home { get; set; }        // 首页地址
    [JsonPropertyName("size")]
    public string Size { get; set; }        // 应用大小
    [JsonPropertyName("fileHash")]
    public string FileHash { get; set; }        // 文件hash
    [JsonPropertyName("permissions")]
    public List<string>? Permissions { get; set; }      // app使用权限情况
    [JsonPropertyName("plugins")]
    public List<string>? Plugins { get; set; }      // app使用插件情况
    [JsonPropertyName("releaseDate")]
    public string ReleaseDate { get; set; }     // 发布时间

    public JmmMetadata(
        Mmid id,
        MainServer server,
        string title = "",
        string shortName = "",
        string icon = "",
        string downloadUrl = "",
        List<string>? images = null,
        string description = "",
        List<string>? author = null,
        string version = "",
        string newFeature = "",
        List<string>? keywords = null,
        string home = "",
        string size = "",
        string fileHash = "",
        List<string>? permissions = null,
        List<string>? plugins = null,
        string releaseDate = "")
    {
        Id = id;
        Server = server;
        Title = title;
        ShortName = shortName;
        Icon = icon;
        DownloadUrl = downloadUrl;
        Images = images;
        Description = description;
        Author = author;
        Version = version;
        NewFeature = newFeature;
        Keywords = keywords;
        Home = home;
        Size = size;
        FileHash = fileHash;
        Permissions = permissions;
        Plugins = plugins;
        ReleaseDate = releaseDate;
    }

    public struct MainServer
    {
        /// <summary>
        /// 应用文件夹的目录
        /// </summary>
        [JsonPropertyName("root")]
        public string Root { get; set; }

        /// <summary>
        /// 入口文件
        /// </summary>
        [JsonPropertyName("entry")]
        public string Entry { get; set; }
    }

    /**
     * <summary>
     * 静态网络服务定义
     * 它将按配置托管一个静态网页服务
     * </summary>
     */
    public struct StaticWebServer
    {
        /// <summary>
        /// 应用文件夹的目录
        /// </summary>
        [JsonPropertyName("root")]
        public string Root { get; set; }

        /// <summary>
        /// 入口文件
        /// </summary>
        [JsonPropertyName("entry")]
        public string Entry { get; set; }

        [JsonPropertyName("subdomain")]
        public string Subdomain { get; set; }

        [JsonPropertyName("port")]
        public int Port { get; set; }

        public StaticWebServer(
            string root,
            string entry = "index.html",
            string subdomain = "cotdemo.bfs.dweb",
            int port = 80)
        {
            Root = root;
            Entry = entry;
            Subdomain = subdomain;
            Port = port;
        }
    }

    public struct OpenWebView
    {
        [JsonPropertyName("url")]
        public string Url { get; set; }

        public OpenWebView(string url = "")
        {
            Url = url;
        }
    }

    public struct SSplashScreen
    {
        [JsonPropertyName("entry")]
        public string? Entry { get; set; }

        public SSplashScreen(string? entry = null)
        {
            Entry = entry;
        }
    }

    public string ToJson() => JsonSerializer.Serialize(this, new JsonSerializerOptions { WriteIndented = true });
    public static JmmMetadata? FromJson(string json) =>
        JsonSerializer.Deserialize<JmmMetadata>(json);
}
