﻿using System.Text.Json;
using System.Text.Json.Serialization;

#nullable enable

namespace DwebBrowser.MicroService.Browser.Jmm;

public class JmmMetadata
{
    [JsonPropertyName("id")]
    public Mmid Id { get; set; }    // jmmApp的id
    [JsonPropertyName("server")]
    public MainServer Server { get; set; }      // 打开应用地址
    [JsonPropertyName("dwebDeeplinks")]
    public List<Dweb_DeepLink>? Dweb_DeepLinks { get; set; }       // dweb-deeplinks
    [JsonPropertyName("name")]
    public string Name { get; set; }       // 应用名称
    [JsonPropertyName("short_name")]
    public string ShortName { get; set; }        // 应用副标题
    [JsonPropertyName("icon")]
    public string Icon { get; set; }        // 应用图标
    [JsonPropertyName("images")]
    public List<string>? Images { get; set; }       // 应用截图
    [JsonPropertyName("description")]
    public string Description { get; set; }        // 应用描述
    [JsonPropertyName("author")]
    public List<string>? Author { get; set; }       // 开发者，作者
    [JsonPropertyName("version")]
    public string Version { get; set; }     // 应用版本
    [JsonPropertyName("new_feature")]
    public string NewFeature { get; set; }      // 新特性，新功能
    [JsonPropertyName("categories")]
    public List<string>? Categories { get; set; }     // 关键词
    [JsonPropertyName("home")]
    public string Home { get; set; }        // 首页地址
    [JsonPropertyName("bundleUrl")]
    public string BundleUrl { get; set; }     // 下载应用地址
    [JsonPropertyName("bundleSize")]
    public string BundleSize { get; set; }        // 应用大小
    [JsonPropertyName("bundleHash")]
    public string BundleHash { get; set; }        // 文件hash
    [JsonPropertyName("permissions")]
    public List<string>? Permissions { get; set; }      // app使用权限情况
    [JsonPropertyName("plugins")]
    public List<string>? Plugins { get; set; }      // app使用插件情况
    [JsonPropertyName("release_date")]
    public string ReleaseDate { get; set; }     // 发布时间

    public JmmMetadata(
        Mmid id,
        MainServer server,
        List<Dweb_DeepLink>? dwebDeeplinks = null,
        string name = "",
        string shortName = "",
        string icon = "",
        List<string>? images = null,
        string description = "",
        List<string>? author = null,
        string version = "",
        string new_feature = "",
        List<string>? categories = null,
        string home = "",
        string bundleUrl = "",
        string bundleSize = "",
        string bundleHash = "",
        List<string>? permissions = null,
        List<string>? plugins = null,
        string release_date = "")
    {
        Id = id;
        Server = server;
        Name = name;
        ShortName = shortName;
        Icon = icon;
        Images = images;
        Description = description;
        Author = author;
        Version = version;
        NewFeature = new_feature;
        Categories = categories;
        Home = home;
        BundleUrl = bundleUrl;
        BundleSize = bundleSize;
        BundleHash = bundleHash;
        Permissions = permissions;
        Plugins = plugins;
        ReleaseDate = release_date;
        if (dwebDeeplinks is null)
        {
            Dweb_DeepLinks = new();
        }
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

    public string ToJson() => JsonSerializer.Serialize(this, new JsonSerializerOptions { WriteIndented = true });
    public static JmmMetadata? FromJson(string json) =>
        JsonSerializer.Deserialize<JmmMetadata>(json);
}
