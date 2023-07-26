namespace DwebBrowser.MicroService.Core;

/**
 * 模块分类
 * 参考链接：
 * 1. https://developer.apple.com/library/archive/documentation/General/Reference/InfoPlistKeyReference/Articles/LaunchServicesKeys.html
 * 2. https://github.com/w3c/manifest/wiki/Categories
 */
[JsonConverter(typeof(MicroModuleCategoryConverter))]
public class MicroModuleCategory : IEquatable<MicroModuleCategory>
{
    [JsonPropertyName("category")]
    public string Category { get; init; }
    public MicroModuleCategory(string category)
    {
        Category = category;
    }

    #region 1. Service 服务
    /** 服务大类
     * > 任何跟服务有关联的请填写该项，用于范索引
     * > 服务拥有后台运行的特征，如果不填写该项，那么程序可能会被当作普通应用程序被直接回收资源
     */
    public static readonly MicroModuleCategory Service = new("service");
    #region 1.1 内核服务
    /** 路由服务
     * > 通常指 `dns.std.dweb` 这个核心，它决策着模块之间通讯的路径
     */
    public static readonly MicroModuleCategory Routing_Service = new("routing-service");
    /** 进程服务
     * > 提供python、js、wasm等语言的运行服务
     * > 和 计算服务 不同，进程服务通常是指 概念上运行在本地 的程序
     */
    public static readonly MicroModuleCategory Process_Service = new("process-service");
    /** 渲染服务
     * > 可视化图形的能力
     * > 比如：Web渲染器、Terminal渲染器、WebGPU渲染器、WebCanvas渲染器 等
     */
    public static readonly MicroModuleCategory Render_Service = new("render-service");
    /** 协议服务
     * > 比如 `http.std.dweb` 这个模块，提供 http/1.1 协议到 Ipc 的映射
     * > 比如 `bluetooth.std.dweb` 这个模块，提供了接口化的 蓝牙协议 管理
     */
    public static readonly MicroModuleCategory Protocol_Service = new("protocol-service");
    /** 设备管理服务
     * > 通常指外部硬件设备
     * > 比如其它的计算机设备、或者通过蓝牙协议管理设备、键盘鼠标打印机等等
     */
    public static readonly MicroModuleCategory Device_Management_Service = new("device-management-service");
    #endregion

    #region 1.2 基础服务

    /** 计算服务
     * > 通常指云计算平台所提供的服务，可以远程部署程序
     */
    public static readonly MicroModuleCategory Computing_Service = new("computing-service");
    /** 存储服务
     * > 比如：文件、对象存储、区块存储
     * > 和数据库的区别是，它不会对存储的内容进行拆解，只能提供基本的写入和读取功能
     */
    public static readonly MicroModuleCategory Storage_Service = new("storage-service");
    /** 数据库服务
     * > 比如：关系型数据库、键值数据库、时序数据库
     * > 和存储服务的区别是，它提供了一套接口来 写入数据、查询数据
     */
    public static readonly MicroModuleCategory Database_Service = new("database-service");
    /** 网络服务
     * > 比如：网关、负载均衡
     */
    public static readonly MicroModuleCategory Network_Service = new("network-service");

    #endregion

    #region 1.3 中间件服务
    /** 聚合服务
     * > 特征：服务编排、服务治理、统一接口、兼容转换
     * > 比如：聚合查询、分布式管理
     */
    public static readonly MicroModuleCategory Hub_Service = new("hub-service");
    /** 分发服务
     * > 特征：减少网络访问的成本、提升网络访问的体验
     * > 比如：CDN、网络加速、文件共享
     */
    public static readonly MicroModuleCategory Distribution_Service = new("distribution-service");
    /** 安全服务
     * > 比如：数据加密、访问控制
     */
    public static readonly MicroModuleCategory Security_Service = new("security-service");

    #endregion

    #region 分析服务
    /** 日志服务 */
    public static readonly MicroModuleCategory Log_Service = new("log-service");
    /** 指标服务 */
    public static readonly MicroModuleCategory Indicator_Service = new("indicator-service");
    /** 追踪服务 */
    public static readonly MicroModuleCategory Tracking_Service = new("tracking-service");

    #endregion

    #region 人工智能服务
    /** 视觉服务 */
    public static readonly MicroModuleCategory Visual_Service = new("visual-service");
    /** 语音服务 */
    public static readonly MicroModuleCategory Audio_Service = new("audio-service");
    /** 文字服务 */
    public static readonly MicroModuleCategory Text_Service = new("text-service");
    /** 机器学习服务 */
    public static readonly MicroModuleCategory Machine_Learning_Service = new("machine-learning-service");

    #endregion

    #endregion

    #region 2. Application 应用
    /** 应用 大类
     * > 如果存在应用特征的模块，都应该填写该项
     * > 应用特征意味着有可视化的图形界面模块，如果不填写该项，那么应用将无法被显示在用户桌面上
     */
    public static readonly MicroModuleCategory Application = new("application");
    #region 2.1 Application 应用 · 系统
    /**
     * 设置
     * > 通常指 `setting.std.dweb` 这个核心，它定义了一种模块管理的标准
     * > 通过这个标准，用户可以在该模块中聚合管理注册的模块
     * > 包括：权限管理、偏好管理、功能开关、主题与个性化、启动程序 等等
     * > 大部分 service 会它们的管理视图注册到该模块中
     */
    public static readonly MicroModuleCategory Settings = new("settings");
    /** 桌面 */
    public static readonly MicroModuleCategory Desktop = new("desktop");
    /** 网页浏览器 */
    public static readonly MicroModuleCategory Web_Browser = new("web-browser");
    /** 文件管理 */
    public static readonly MicroModuleCategory Files = new("files");
    /** 钱包 */
    public static readonly MicroModuleCategory Wallet = new("wallet");
    /** 助理
     * > 该类应用通常拥有极高的权限，比如 屏幕阅读工具、AI助理工具 等
     */
    public static readonly MicroModuleCategory Assistant = new("assistant");
    #endregion

    #region 2.2 Application 应用 · 工作效率
    /** 商业 */
    public static readonly MicroModuleCategory Business = new("business");
    /** 开发者工具 */
    public static readonly MicroModuleCategory Developer = new("developer");
    /** 教育 */
    public static readonly MicroModuleCategory Education = new("education");
    /** 财务 */
    public static readonly MicroModuleCategory Finance = new("finance");
    /** 办公效率 */
    public static readonly MicroModuleCategory Productivity = new("productivity");
    /** 消息软件
     * > 讯息、邮箱
     */
    public static readonly MicroModuleCategory Messages = new("messages");
    /** 实时互动 */
    public static readonly MicroModuleCategory Live = new("live");
    #endregion

    #region 2.3 Application 应用 · 娱乐
    /** 娱乐 */
    public static readonly MicroModuleCategory Entertainment = new("entertainment");
    /** 游戏 */
    public static readonly MicroModuleCategory Games = new("games");
    /** 生活休闲 */
    public static readonly MicroModuleCategory Lifestyle = new("lifestyle");
    /** 音乐 */
    public static readonly MicroModuleCategory Music = new("music");
    /** 新闻 */
    public static readonly MicroModuleCategory News = new("news");
    /** 体育 */
    public static readonly MicroModuleCategory Sports = new("sports");
    /** 视频 */
    public static readonly MicroModuleCategory Video = new("video");
    /** 照片 */
    public static readonly MicroModuleCategory Photo = new("photo");
    #endregion

    #region 2.4 Application 应用 · 创意
    /** 图形和设计 */
    public static readonly MicroModuleCategory Graphics_a_Design = new("graphics-design");
    /** 摄影与录像 */
    public static readonly MicroModuleCategory Photography = new("photography");
    /** 个性化 */
    public static readonly MicroModuleCategory Personalization = new("personalization");
    #endregion

    #region 2.5 Application 应用 · 实用工具
    /** 书籍 */
    public static readonly MicroModuleCategory Books = new("books");
    /** 杂志 */
    public static readonly MicroModuleCategory Magazines = new("magazines");
    /** 食物 */
    public static readonly MicroModuleCategory Food = new("food");
    /** 健康 */
    public static readonly MicroModuleCategory Health = new("health");
    /** 健身 */
    public static readonly MicroModuleCategory Fitness = new("fitness");
    /** 医疗 */
    public static readonly MicroModuleCategory Medical = new("medical");
    /** 导航 */
    public static readonly MicroModuleCategory Navigation = new("navigation");
    /** 参考工具 */
    public static readonly MicroModuleCategory Reference = new("reference");
    /** 实用工具 */
    public static readonly MicroModuleCategory Utilities = new("utilities");
    /** 旅行 */
    public static readonly MicroModuleCategory Travel = new("travel");
    /** 天气 */
    public static readonly MicroModuleCategory Weather = new("weather");
    /** 儿童 */
    public static readonly MicroModuleCategory Kids = new("kids");
    /** 购物 */
    public static readonly MicroModuleCategory Shopping = new("shopping");
    /** 安全 */
    public static readonly MicroModuleCategory Security = new("security");
    #endregion

    #region 2.6 Application 应用 · 社会
    /** 社交网络 */
    public static readonly MicroModuleCategory Social = new("social");
    /** 职业生涯 */
    public static readonly MicroModuleCategory Career = new("career");
    /** 政府 */
    public static readonly MicroModuleCategory Government = new("government");
    /** 政治 */
    public static readonly MicroModuleCategory Politics = new("politics");
    #endregion

    #endregion

    #region 3. Game 游戏（属于应用的细分）
    /** 动作游戏 */
    public static readonly MicroModuleCategory Action_Games = new("action-games");
    /** 冒险游戏 */
    public static readonly MicroModuleCategory Adventure_Games = new("adventure-games");
    /** 街机游戏 */
    public static readonly MicroModuleCategory Arcade_Games = new("arcade-games");
    /** 棋盘游戏 */
    public static readonly MicroModuleCategory Board_Games = new("board-games");
    /** 卡牌游戏 */
    public static readonly MicroModuleCategory Card_Games = new("card-games");
    /** 赌场游戏 */
    public static readonly MicroModuleCategory Casino_Games = new("casino-games");
    /** 骰子游戏 */
    public static readonly MicroModuleCategory Dice_Games = new("dice-games");
    /** 教育游戏 */
    public static readonly MicroModuleCategory Educational_Games = new("educational-games");
    /** 家庭游戏 */
    public static readonly MicroModuleCategory Family_Games = new("family-games");
    /** 儿童游戏 */
    public static readonly MicroModuleCategory Kids_Games = new("kids-games");
    /** 音乐游戏 */
    public static readonly MicroModuleCategory Music_Games = new("music-games");
    /** 益智游戏 */
    public static readonly MicroModuleCategory Puzzle_Games = new("puzzle-games");
    /** 赛车游戏 */
    public static readonly MicroModuleCategory Racing_Games = new("racing-games");
    /** 角色扮演游戏 */
    public static readonly MicroModuleCategory Role_Playing_Games = new("role-playing-games");
    /** 模拟经营游戏 */
    public static readonly MicroModuleCategory Simulation_Games = new("simulation-games");
    /** 运动游戏 */
    public static readonly MicroModuleCategory Sports_Games = new("sports-games");
    /** 策略游戏 */
    public static readonly MicroModuleCategory Strategy_Games = new("strategy-games");
    /** 问答游戏 */
    public static readonly MicroModuleCategory Trivia_Games = new("trivia-games");
    /** 文字游戏 */
    public static readonly MicroModuleCategory Word_Games = new("word-games");
    #endregion

    /// <summary>
    /// Serialize MicroModuleCategory
    /// </summary>
    /// <returns>JSON string representation of the MicroModuleCategory</returns>
    public string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize MicroModuleCategory
    /// </summary>
    /// <param name="json">JSON string representation of MicroModuleCategory</param>
    /// <returns>An instance of a MicroModuleCategory object.</returns>
    public static MicroModuleCategory? FromJson(string json) => JsonSerializer.Deserialize<MicroModuleCategory>(json);

    public bool Equals(MicroModuleCategory? other)
    {
        return GetHashCode() == other?.GetHashCode();
    }

    public override int GetHashCode()
    {
        return Category.GetHashCode();
    }
}

#region MicroModuleCategory序列化反序列化
public class MicroModuleCategoryConverter : JsonConverter<MicroModuleCategory>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") is not null && typeToConvert.GetMethod("FromJson") is not null;

    public override MicroModuleCategory? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        var category = reader.GetString();
        return category switch
        {
            "service" => MicroModuleCategory.Service,
            "routing-service" => MicroModuleCategory.Routing_Service,
            "process-service" => MicroModuleCategory.Process_Service,
            "render-service" => MicroModuleCategory.Render_Service,
            "protocol-service" => MicroModuleCategory.Protocol_Service,
            "device-management-service" => MicroModuleCategory.Device_Management_Service,
            "computing-service" => MicroModuleCategory.Computing_Service,
            "storage-service" => MicroModuleCategory.Storage_Service,
            "database-service" => MicroModuleCategory.Database_Service,
            "network-service" => MicroModuleCategory.Network_Service,
            "hub-service" => MicroModuleCategory.Hub_Service,
            "distribution-service" => MicroModuleCategory.Distribution_Service,
            "security-service" => MicroModuleCategory.Security_Service,
            "log-service" => MicroModuleCategory.Log_Service,
            "indicator-service" => MicroModuleCategory.Indicator_Service,
            "tracking-service" => MicroModuleCategory.Tracking_Service,
            "visual-service" => MicroModuleCategory.Visual_Service,
            "audio-service" => MicroModuleCategory.Audio_Service,
            "text-service" => MicroModuleCategory.Text_Service,
            "machine-learning-service" => MicroModuleCategory.Machine_Learning_Service,
            "application" => MicroModuleCategory.Application,
            "settings" => MicroModuleCategory.Settings,
            "desktop" => MicroModuleCategory.Desktop,
            "web-browser" => MicroModuleCategory.Web_Browser,
            "files" => MicroModuleCategory.Files,
            "wallet" => MicroModuleCategory.Wallet,
            "assistant" => MicroModuleCategory.Assistant,
            "business" => MicroModuleCategory.Business,
            "developer" => MicroModuleCategory.Developer,
            "education" => MicroModuleCategory.Education,
            "finance" => MicroModuleCategory.Finance,
            "productivity" => MicroModuleCategory.Productivity,
            "messages" => MicroModuleCategory.Messages,
            "live" => MicroModuleCategory.Live,
            "entertainment" => MicroModuleCategory.Entertainment,
            "games" => MicroModuleCategory.Games,
            "lifestyle" => MicroModuleCategory.Lifestyle,
            "music" => MicroModuleCategory.Music,
            "news" => MicroModuleCategory.News,
            "sports" => MicroModuleCategory.Sports,
            "video" => MicroModuleCategory.Video,
            "photo" => MicroModuleCategory.Photo,
            "graphics-design" => MicroModuleCategory.Graphics_a_Design,
            "photography" => MicroModuleCategory.Photography,
            "personalization" => MicroModuleCategory.Personalization,
            "books" => MicroModuleCategory.Books,
            "magazines" => MicroModuleCategory.Magazines,
            "food" => MicroModuleCategory.Food,
            "health" => MicroModuleCategory.Health,
            "fitness" => MicroModuleCategory.Fitness,
            "medical" => MicroModuleCategory.Medical,
            "navigation" => MicroModuleCategory.Navigation,
            "reference" => MicroModuleCategory.Reference,
            "utilities" => MicroModuleCategory.Utilities,
            "travel" => MicroModuleCategory.Travel,
            "weather" => MicroModuleCategory.Weather,
            "kids" => MicroModuleCategory.Kids,
            "shopping" => MicroModuleCategory.Shopping,
            "security" => MicroModuleCategory.Security,
            "social" => MicroModuleCategory.Social,
            "career" => MicroModuleCategory.Career,
            "government" => MicroModuleCategory.Government,
            "politics" => MicroModuleCategory.Politics,
            "action-games" => MicroModuleCategory.Action_Games,
            "adventure-games" => MicroModuleCategory.Adventure_Games,
            "arcade-games" => MicroModuleCategory.Arcade_Games,
            "board-games" => MicroModuleCategory.Board_Games,
            "card-games" => MicroModuleCategory.Card_Games,
            "casino-games" => MicroModuleCategory.Casino_Games,
            "dice-games" => MicroModuleCategory.Dice_Games,
            "educational-games" => MicroModuleCategory.Educational_Games,
            "family-games" => MicroModuleCategory.Family_Games,
            "kids-games" => MicroModuleCategory.Kids_Games,
            "music-games" => MicroModuleCategory.Music_Games,
            "puzzle-games" => MicroModuleCategory.Puzzle_Games,
            "racing-games" => MicroModuleCategory.Racing_Games,
            "role-playing-games" => MicroModuleCategory.Role_Playing_Games,
            "simulation-games" => MicroModuleCategory.Simulation_Games,
            "sports-games" => MicroModuleCategory.Sports_Games,
            "strategy-games" => MicroModuleCategory.Strategy_Games,
            "trivia-games" => MicroModuleCategory.Trivia_Games,
            "word-games" => MicroModuleCategory.Word_Games,
            _ => throw new JsonException("Invalid MicroModule Category")
        };
    }

    public override void Write(Utf8JsonWriter writer, MicroModuleCategory value, JsonSerializerOptions options)
    {
        writer.WriteStringValue(value.Category);
    }
}
#endregion

#region JsonStringEnumConverter 不支持 Native AOT
//[JsonConverter(typeof(JsonStringEnumConverter<MicroModuleCategory>))]
//public enum MicroModuleCategory
//{
//    #region 1. Service 服务
//    /** 服务大类
//     * > 任何跟服务有关联的请填写该项，用于范索引
//     * > 服务拥有后台运行的特征，如果不填写该项，那么程序可能会被当作普通应用程序被直接回收资源
//     */
//    Service,
//    #region 1.1 内核服务
//    /** 路由服务
//     * > 通常指 `dns.std.dweb` 这个核心，它决策着模块之间通讯的路径
//     */
//    Routing_Service,
//    /** 进程服务
//     * > 提供python、js、wasm等语言的运行服务
//     * > 和 计算服务 不同，进程服务通常是指 概念上运行在本地 的程序
//     */
//    Process_Service,
//    /** 渲染服务
//     * > 可视化图形的能力
//     * > 比如：Web渲染器、Terminal渲染器、WebGPU渲染器、WebCanvas渲染器 等
//     */
//    Render_Service,
//    /** 协议服务
//     * > 比如 `http.std.dweb` 这个模块，提供 http/1.1 协议到 Ipc 的映射
//     * > 比如 `bluetooth.std.dweb` 这个模块，提供了接口化的 蓝牙协议 管理
//     */
//    Protocol_Service,
//    /** 设备管理服务
//     * > 通常指外部硬件设备
//     * > 比如其它的计算机设备、或者通过蓝牙协议管理设备、键盘鼠标打印机等等
//     */
//    Device_Management_Service,
//    #endregion

//    #region 1.2 基础服务

//    /** 计算服务
//     * > 通常指云计算平台所提供的服务，可以远程部署程序
//     */
//    Computing_Service,
//    /** 存储服务
//     * > 比如：文件、对象存储、区块存储
//     * > 和数据库的区别是，它不会对存储的内容进行拆解，只能提供基本的写入和读取功能
//     */
//    Storage_Service,
//    /** 数据库服务
//     * > 比如：关系型数据库、键值数据库、时序数据库
//     * > 和存储服务的区别是，它提供了一套接口来 写入数据、查询数据
//     */
//    Database_Service,
//    /** 网络服务
//     * > 比如：网关、负载均衡
//     */
//    Network_Service,

//    #endregion

//    #region 1.3 中间件服务
//    /** 聚合服务
//     * > 特征：服务编排、服务治理、统一接口、兼容转换
//     * > 比如：聚合查询、分布式管理
//     */
//    Hub_Service,
//    /** 分发服务
//     * > 特征：减少网络访问的成本、提升网络访问的体验
//     * > 比如：CDN、网络加速、文件共享
//     */
//    Distribution_Service,
//    /** 安全服务
//     * > 比如：数据加密、访问控制
//     */
//    Security_Service,

//    #endregion

//    #region 分析服务
//    /** 日志服务 */
//    Log_Service,
//    /** 指标服务 */
//    Indicator_Service,
//    /** 追踪服务 */
//    Tracking_Service,

//    #endregion

//    #region 人工智能服务
//    /** 视觉服务 */
//    Visual_Service,
//    /** 语音服务 */
//    Audio_Service,
//    /** 文字服务 */
//    Text_Service,
//    /** 机器学习服务 */
//    Machine_Learning_Service,

//    #endregion

//    #endregion

//    #region 2. Application 应用
//    /** 应用 大类
//     * > 如果存在应用特征的模块，都应该填写该项
//     * > 应用特征意味着有可视化的图形界面模块，如果不填写该项，那么应用将无法被显示在用户桌面上
//     */
//    Application,
//    #region 2.1 Application 应用 · 系统
//    /**
//     * 设置
//     * > 通常指 `setting.std.dweb` 这个核心，它定义了一种模块管理的标准
//     * > 通过这个标准，用户可以在该模块中聚合管理注册的模块
//     * > 包括：权限管理、偏好管理、功能开关、主题与个性化、启动程序 等等
//     * > 大部分 service 会它们的管理视图注册到该模块中
//     */
//    Settings,
//    /** 桌面 */
//    Desktop,
//    /** 网页浏览器 */
//    Web_Browser,
//    /** 文件管理 */
//    Files,
//    /** 钱包 */
//    Wallet,
//    /** 助理
//     * > 该类应用通常拥有极高的权限，比如 屏幕阅读工具、AI助理工具 等
//     */
//    Assistant,
//    #endregion

//    #region 2.2 Application 应用 · 工作效率
//    /** 商业 */
//    Business,
//    /** 开发者工具 */
//    Developer,
//    /** 教育 */
//    Education,
//    /** 财务 */
//    Finance,
//    /** 办公效率 */
//    Productivity,
//    /** 消息软件
//     * > 讯息、邮箱
//     */
//    Messages,
//    /** 实时互动 */
//    Live,
//    #endregion

//    #region 2.3 Application 应用 · 娱乐
//    /** 娱乐 */
//    Entertainment,
//    /** 游戏 */
//    Games,
//    /** 生活休闲 */
//    Lifestyle,
//    /** 音乐 */
//    Music,
//    /** 新闻 */
//    News,
//    /** 体育 */
//    Sports,
//    /** 视频 */
//    Video,
//    /** 照片 */
//    Photo,
//    #endregion

//    #region 2.4 Application 应用 · 创意
//    /** 图形和设计 */
//    Graphics_a_Design,
//    /** 摄影与录像 */
//    Photography,
//    /** 个性化 */
//    Personalization,
//    #endregion

//    #region 2.5 Application 应用 · 实用工具
//    /** 书籍 */
//    Books,
//    /** 杂志 */
//    Magazines,
//    /** 食物 */
//    Food,
//    /** 健康 */
//    Health,
//    /** 健身 */
//    Fitness,
//    /** 医疗 */
//    Medical,
//    /** 导航 */
//    Navigation,
//    /** 参考工具 */
//    Reference,
//    /** 实用工具 */
//    Utilities,
//    /** 旅行 */
//    Travel,
//    /** 天气 */
//    Weather,
//    /** 儿童 */
//    Kids,
//    /** 购物 */
//    Shopping,
//    /** 安全 */
//    Security,
//    #endregion

//    #region 2.6 Application 应用 · 社会
//    /** 社交网络 */
//    Social,
//    /** 职业生涯 */
//    Career,
//    /** 政府 */
//    Government,
//    /** 政治 */
//    Politics,
//    #endregion

//    #endregion

//    #region 3. Game 游戏（属于应用的细分）
//    /** 动作游戏 */
//    Action_Games,
//    /** 冒险游戏 */
//    Adventure_Games,
//    /** 街机游戏 */
//    Arcade_Games,
//    /** 棋盘游戏 */
//    Board_Games,
//    /** 卡牌游戏 */
//    Card_Games,
//    /** 赌场游戏 */
//    Casino_Games,
//    /** 骰子游戏 */
//    Dice_Games,
//    /** 教育游戏 */
//    Educational_Games,
//    /** 家庭游戏 */
//    Family_Games,
//    /** 儿童游戏 */
//    Kids_Games,
//    /** 音乐游戏 */
//    Music_Games,
//    /** 益智游戏 */
//    Puzzle_Games,
//    /** 赛车游戏 */
//    Racing_Games,
//    /** 角色扮演游戏 */
//    Role_Playing_Games,
//    /** 模拟经营游戏 */
//    Simulation_Games,
//    /** 运动游戏 */
//    Sports_Games,
//    /** 策略游戏 */
//    Strategy_Games,
//    /** 问答游戏 */
//    Trivia_Games,
//    /** 文字游戏 */
//    Word_Games,
//    #endregion
//}
#endregion