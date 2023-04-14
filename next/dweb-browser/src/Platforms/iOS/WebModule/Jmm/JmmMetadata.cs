using System.Text.Json;
using System.Text.Json.Serialization;

namespace DwebBrowser.WebModule.Jmm;

public class JmmMetadata
{
    [JsonPropertyName("id")]
    public Mmid Id { get; set; }
    [JsonPropertyName("server")]
    public MainServer Server { get; set; }
    [JsonPropertyName("title")]
    public string Title { get; set; }
    [JsonPropertyName("subtitle")]
    public string Subtitle { get; set; }
    [JsonPropertyName("icon")]
    public string Icon { get; set; }
    [JsonPropertyName("downloadUrl")]
    public string DownloadUrl { get; set; }
    [JsonPropertyName("images")]
    public List<string>? Images { get; set; }
    [JsonPropertyName("introduction")]
    public string Introduction { get; set; }
    [JsonPropertyName("splashScreen")]
    public SSplashScreen SplashScreen { get; set; }
    [JsonPropertyName("author")]
    public List<string>? Author { get; set; }
    [JsonPropertyName("version")]
    public string Version { get; set; }
    [JsonPropertyName("keywords")]
    public List<string>? Keywords { get; set; }
    [JsonPropertyName("home")]
    public string Home { get; set; }
    [JsonPropertyName("size")]
    public string Size { get; set; }
    [JsonPropertyName("fileHash")]
    public string FileHash { get; set; }
    [JsonPropertyName("permissions")]
    public List<string>? Permissions { get; set; }
    [JsonPropertyName("plugins")]
    public List<string>? Plugins { get; set; }
    [JsonPropertyName("releaseDate")]
    public string ReleaseDate { get; set; }
    [JsonPropertyName("staticWebServers")]
    public List<StaticWebServer> StaticWebServers { get; set; }
    [JsonPropertyName("openWebViewList")]
    public List<OpenWebView> OpenWebViewList { get; set; }

    public JmmMetadata(
        Mmid id,
        MainServer server,
        string title = "",
        string subtitle = "",
        string icon = "",
        string downloadUrl = "",
        List<string>? images = null,
        string introduction = "",
        SSplashScreen splashScreen = new SSplashScreen(),
        List<string>? author = null,
        string version = "",
        List<string>? keywords = null,
        string home = "",
        string size = "",
        string fileHash = "",
        List<string>? permissions = null,
        List<string>? plugins = null,
        string releaseDate = "",
        List<StaticWebServer>? staticWebServers = null,
        List<OpenWebView>? openWebViewList = null)
    {
        Id = id;
        Server = server;
        Title = title;
        Subtitle = subtitle;
        Icon = icon;
        DownloadUrl = downloadUrl;
        Images = images;
        Introduction = introduction;
        SplashScreen = splashScreen;
        Author = author;
        Version = version;
        Keywords = keywords;
        Home = home;
        Size = size;
        FileHash = fileHash;
        Permissions = permissions;
        Plugins = plugins;
        ReleaseDate = releaseDate;
        StaticWebServers = staticWebServers ?? new List<StaticWebServer>();
        OpenWebViewList = openWebViewList ?? new List<OpenWebView>();
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

    public string ToJson() => JsonSerializer.Serialize<JmmMetadata>(
        this, new JsonSerializerOptions { WriteIndented = true });
    public static JmmMetadata? FromJson(string json) =>
        JsonSerializer.Deserialize<JmmMetadata>(json);
}
