namespace DwebBrowser.MicroService.Core;

/// <summary>
/// 一种通用的 “应用” 元数据格式
/// </summary>
public interface ICommonAppManifest
{
    public string? Version { get; set; }
    public TextDirectionType? Dir { get; set; }
    public string? Lang { get; set; }
    public string? Name { get; set; }
    public string? ShortName { get; set; }
    public string? Description { get; set; }
    public List<ImageResource>? Icons { get; set; }
    public List<ImageResource>? Screenshots { get; set; }
    public List<MicroModuleCategory>? Categories { get; set; }
    public DisplayModeType? Display { get; set; }
    public OrientationType? Orientation { get; set; }
    public string? Id { get; set; }
    public string? ThemeColor { get; set; }
    public string? BackgroundColor { get; set; }
    public List<ShortcutItem>? Shortcuts { get; set; }
}

public interface IMicroModuleManifest
{
    /// <summary>
    /// 模块id
    /// </summary>
    public Mmid Mmid { get; init; }
    /// <summary>
    /// 对通讯协议的支持情况
    /// </summary>
    public IpcSupportProtocols IpcSupportProtocols { get; init; }
    /// <summary>
    /// 匹配的“DWEB深层链接”
    /// 取代明确的 mmid，dweb-deeplinks 可以用来表征一种特性、一种共识，它必须是 'dweb:{domain}[/pathname[/pathname...]]' 的格式规范
    /// 为了交付给用户清晰的可管理的模式，这里的 deeplink 仅仅允许精确的前缀匹配，因此我们通常会规范它的动作层级依次精确
    /// 
    /// 比如说：'dweb:mailto'，那么在面对 'dweb:mailto?address=someone@mail.com&title=xxx' 的链接时，该链接会被打包成一个 IpcRequest 消息传输过来
    /// 比如说：'dweb:open/file/image'，那么就会匹配这样的链接 'dweb:open/file/image/svg?uri=file:///example.svg'
    /// 
    /// dweb_deeplinks 由 dns 模块进行统一管理，也由它提供相关的管理界面、控制策略
    /// </summary>
    public List<Dweb_DeepLink> Dweb_deeplinks { get; init; }
    public List<MicroModuleCategory> Categories { get; init; }
    public string Name { get; set; }                                               /// <see cref="https://w3c.github.io/manifest/#name-member"/>
    public string? Version { get; set; }
    public TextDirectionType? Dir { get; set; }                                    /// <see cref="https://w3c.github.io/manifest/#dir-member"/>
    public string? Lang { get; set; }                                              /// <see cref="https://w3c.github.io/manifest/#lang-member"/>
    public string? ShortName { get; set; }                                         /// <see cref="https://w3c.github.io/manifest/#short_name-member"/>
    public string? Description { get; set; }                                       /// <see cref="https://w3c.github.io/manifest/#description-member"/>
    public List<ImageResource>? Icons { get; set; }                                  /// <see cref="https://w3c.github.io/manifest/#icons-member"/>
    public List<ImageResource>? Screenshots { get; set; }                            /// <see cref="https://w3c.github.io/manifest/#screenshots-member"/>
    public DisplayModeType? Display { get; set; }                                  /// <see cref="https://w3c.github.io/manifest/#display-member"/>
    public OrientationType? Orientation { get; set; }                              /// <see cref="https://w3c.github.io/manifest/#orientation-member"/>
    public string? ThemeColor { get; set; }                                        /// <see cref="https://w3c.github.io/manifest/#theme_color-member"/>
    public string? BackgroundColor { get; set; }                                   /// <see cref="https://w3c.github.io/manifest/#background_color-member"/>
    public List<ShortcutItem>? Shortcuts { get; set; }                             /// <see cref="https://w3c.github.io/manifest/#shortcuts-member"/>
}

public interface IMicroModule : IMicroModuleManifest
{
    /// <summary>
    /// 添加双工连接到自己的池子中，但自己销毁，这些双工连接都会被断掉
    /// </summary>
    /// <param name="ipc"></param>
    public void AddToIpcSet(Ipc ipc);
}

#region TextDirectionType Enum Type
#region TextDirectionType
[JsonConverter(typeof(TextDirectionConverter))]
public class TextDirectionType
{
    [JsonPropertyName("direction")]
    public string Direction { get; init; }

    public TextDirectionType(string direction)
    {
        Direction = direction;
    }

    public static readonly TextDirectionType Ltr = new("ltr");
    public static readonly TextDirectionType Rtr = new("rtl");
    public static readonly TextDirectionType Auto = new("auto");

    /// <summary>
    /// Serialize TextDirectionType
    /// </summary>
    /// <returns>JSON string representation of the TextDirectionType</returns>
    public string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize TextDirectionType
    /// </summary>
    /// <param name="json">JSON string representation of TextDirectionType</param>
    /// <returns>An instance of a TextDirectionType object.</returns>
    public static TextDirectionType? FromJson(string json) => JsonSerializer.Deserialize<TextDirectionType>(json);
}
#endregion

#region TextDirectionType序列化反序列化
public class TextDirectionConverter : JsonConverter<TextDirectionType>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") is not null && typeToConvert.GetMethod("FromJson") is not null;

    public override TextDirectionType? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        var direction = reader.GetString();
        return direction switch
        {
            "ltr" => TextDirectionType.Ltr,
            "rtl" => TextDirectionType.Rtr,
            "auto" => TextDirectionType.Auto,
            _ => throw new JsonException("Invalid TextDirection Type")
        };

    }

    public override void Write(Utf8JsonWriter writer, TextDirectionType value, JsonSerializerOptions options)
    {
        writer.WriteStringValue(value.Direction);
    }
}
#endregion
#endregion

#region DisplayModeType Enum Type
#region DisplayModeType
[JsonConverter(typeof(DisplayModeConverter))]
public class DisplayModeType
{
    [JsonPropertyName("mode")]
    public string Mode { get; init; }

    public DisplayModeType(string mode)
    {
        Mode = mode;
    }

    public static readonly DisplayModeType Fullscreen = new("fullscreen");
    public static readonly DisplayModeType Standalone = new("standalone");
    public static readonly DisplayModeType MinimalUi = new("minimal-ui");
    public static readonly DisplayModeType Browser = new("browser");

    /// <summary>
    /// Serialize DisplayModeType
    /// </summary>
    /// <returns>JSON string representation of the DisplayModeType</returns>
    public string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize DisplayModeType
    /// </summary>
    /// <param name="json">JSON string representation of DisplayModeType</param>
    /// <returns>An instance of a DisplayModeType object.</returns>
    public static DisplayModeType? FromJson(string json) => JsonSerializer.Deserialize<DisplayModeType>(json);
}
#endregion

#region DisplayModeType序列化反序列化
public class DisplayModeConverter : JsonConverter<DisplayModeType>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") is not null && typeToConvert.GetMethod("FromJson") is not null;

    public override DisplayModeType? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        var direction = reader.GetString();
        return direction switch
        {
            "fullscreen" => DisplayModeType.Fullscreen,
            "standalone" => DisplayModeType.Standalone,
            "minimal-ui" => DisplayModeType.MinimalUi,
            "browser" => DisplayModeType.Browser,
            _ => throw new JsonException("Invalid DisplayMode Type")
        };

    }

    public override void Write(Utf8JsonWriter writer, DisplayModeType value, JsonSerializerOptions options)
    {
        writer.WriteStringValue(value.Mode);
    }
}
#endregion

#endregion

#region OrientationType Enum Type
#region OrientationType
[JsonConverter(typeof(OrientationConverter))]
public class OrientationType
{
    [JsonPropertyName("orientation")]
    public string Orientation { get; init; }

    public OrientationType(string orientation)
    {
        Orientation = orientation;
    }

    public static readonly OrientationType Any = new("any");
    public static readonly OrientationType Landscape = new("landscape");
    public static readonly OrientationType LandscapePrimary = new("landscape-primary");
    public static readonly OrientationType LandscapeSecondary = new("landscape-secondary");
    public static readonly OrientationType Natural = new("natural");
    public static readonly OrientationType Portrait = new("portrait");
    public static readonly OrientationType PortraitPrimary = new("portrait-primary");
    public static readonly OrientationType PortraitSecondary = new("portrait-secondary");

    /// <summary>
    /// Serialize OrientationType
    /// </summary>
    /// <returns>JSON string representation of the OrientationType</returns>
    public string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize OrientationType
    /// </summary>
    /// <param name="json">JSON string representation of OrientationType</param>
    /// <returns>An instance of a OrientationType object.</returns>
    public static OrientationType? FromJson(string json) => JsonSerializer.Deserialize<OrientationType>(json);
}
#endregion

#region OrientationType序列化反序列化
public class OrientationConverter : JsonConverter<OrientationType>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") is not null && typeToConvert.GetMethod("FromJson") is not null;

    public override OrientationType? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        var direction = reader.GetString();
        return direction switch
        {
            "any" => OrientationType.Any,
            "landscape" => OrientationType.Landscape,
            "landscape-primary" => OrientationType.LandscapePrimary,
            "landscape-secondary" => OrientationType.LandscapeSecondary,
            "natural" => OrientationType.Natural,
            "portrait" => OrientationType.Portrait,
            "portrait-primary" => OrientationType.PortraitPrimary,
            "portrait-secondary" => OrientationType.PortraitSecondary,
            _ => throw new JsonException("Invalid DisplayMode Type")
        };

    }

    public override void Write(Utf8JsonWriter writer, OrientationType value, JsonSerializerOptions options)
    {
        writer.WriteStringValue(value.Orientation);
    }
}
#endregion
#endregion

/// <summary>
/// Each `Fingerprints` represents a set of cryptographic fingerprints used for verifying the application. A
/// fingerprint has the following two properties: `type` and `value`.
/// </summary>
/// <param name="type"></param>
/// <param name="value"></param>
/// <see cref="https://w3c.github.io/manifest/#fingerprints-member"/>
public record Fingerprint(
    string? type = null,
    string? value = null
);

/// <summary>
/// Each `ExternalApplicationResources` represents an application related to the web application.
/// </summary>
/// <param name="platform">The `platform` member represents the platform to which a containing object applies.</param>
/// <param name="url">The `url` member of an ExternalApplicationResource dictionary represents the URL at which the application can be found.</param>
/// <param name="id">The `id` member of an ExternalApplicationResource dictionary represents the id which is used to represent the application on the platform.</param>
/// <param name="min_version">The `min_version` member of an `ExternalApplicationResource` dictionary represents the minimum version of the application that is considered related to this web app.</param>
/// <param name="fingerprints">The `fingerprints` member of an `ExternalApplicationResource` dictionary represents an array of `Fingerprint`s.</param>
/// <see cref="https://w3c.github.io/manifest/#externalapplicationresource-and-its-members"/>
public record ExternalApplicationResource(
    string platform,                   /// <see cref="https://w3c.github.io/manifest/#platform-member-0"/>
    string? url = null,                /// <see cref="https://w3c.github.io/manifest/#url-member-0"/>
    string? id = null,                 /// <see cref="https://w3c.github.io/manifest/#id-member"/>
    string? min_version = null,        /// <see cref="https://w3c.github.io/manifest/#min_version-member"/>
    Fingerprint[]? fingerprints = null /// <see cref="https://w3c.github.io/manifest/#fingerprints-member"/>
);

/// <summary>
/// Each `ShortcutItem` represents a link to a key task or page within a web app.
/// </summary>
/// <param name="name">The `name` member of a `ShortcutItem` is a `string` that represents the name of the shortcut as it is usually displayed to the user in a context menu.</param>
/// <param name="url">The `url` member of a `ShortcutItem` is the URL within the application's scope that opens when the associated shortcut is activated.</param>
/// <param name="short_name">The `short_name` member of a `ShortcutItem` is a `string` that represents a short version of the name of the shortcut.</param>
/// <param name="description">The `description` member of a `ShortcutItem` is a `string` that allows the developer to describe the purpose of the shortcut.</param>
/// <param name="icons">The `icons` member of an `ShortcutItem` member is an `array` of `ImageResource`s that can serve as iconic representations of the shortcut in various contexts.</param>
/// <see cref="https://w3c.github.io/manifest/#shortcutitem-and-its-members"/>
public record ShortcutItem(
    string name,                /// <see cref="https://w3c.github.io/manifest/#name-member-0"/>
    string url,                 /// <see cref="https://w3c.github.io/manifest/#url-member"/>
    string? short_name = null,  /// <see cref="https://w3c.github.io/manifest/#short_name-member-0"/>
    string? description = null, /// <see cref="https://w3c.github.io/manifest/#description-member-0"/>
    List<ImageResource>? icons = null /// <see cref="https://w3c.github.io/manifest/#icons-member-0"/>
);

/// <summary>
/// A `manifest` is a JSON document that contains startup parameters and application defaults for
/// when a web application is launched.
/// </summary>
/// <param name="dir">The `dir` member specifies the base direction for the directionality-capable members of the manifest.</param>
/// <param name="lang">
/// The `lang` member is a language tag (`string`) that specifies the primary language for the values of
/// the manifest's directionality-capable members (as knowing the language can also help with directionality).
/// </param>
/// <param name="name">
/// The `name` member is a `string` that represents the name of the web application as it is usually displayed
/// to the user (e.g., amongst a list of other applications, or as a label for an icon).
/// </param>
/// <param name="short_name">
/// The `short_name` member is a `string` that represents a short version of the name of the web application.
/// </param>
/// <param name="description">
/// The `description` member allows the developer to describe the purpose of the web application.
/// </param>
/// <param name="icons">
/// The `icons` member is an array of `ImageResource`s that can serve as iconic representations of the web
/// application in various contexts.
/// </param>
/// <param name="screenshots">
/// The `screenshots` member is an array of `ImageResource`s, representing the web application in common
/// usage scenarios.
/// </param>
/// <param name="categories">
/// The `categories` member describes the expected application categories to which the web application belongs.
/// </param>
/// <param name="iarc_rating_id">
/// The `iarc_rating_id` member is a `string` that represents the International Age Rating Coalition (IARC)
/// certification code of the web application.
/// </param>
/// <param name="start_url">
/// The `start_url` member is a `string` that represents the start URL , which is URL that the developer
/// would prefer the user agent load when the user launches the web application (e.g., when the user
/// clicks on the icon of the web application from a device's application menu or homescreen).
/// </param>
/// <param name="display">
/// The `display` member is a `DisplayModeType`, whose value is one of display modes values.
/// </param>
/// <param name="orientation">
/// The `orientation` member is a string that serves as the default screen orientation for all top-level
/// browsing contexts of the web application.
/// </param>
/// <param name="id">
/// The manifest's id member is a string that represents the identity for the application.
/// The identity takes the form of a URL, which is same origin as the start URL.
/// </param>
/// <param name="theme_color">
/// The `theme_color` member serves as the default theme color for an application context.
/// </param>
/// <param name="background_color">
/// The `background_color` member describes the expected background color of the web application.
/// </param>
/// <param name="scope">
/// The `scope` member is a string that represents the navigation scope of this web application's
/// application context.
/// </param>
/// <param name="related_applications">
/// The `related_applications` member lists related applications and serves as an indication of such a
/// relationship between web application and related applications.
/// </param>
/// <param name="prefer_related_applications">
/// The `prefer_related_applications` member is a boolean value that is used as a hint for the user agent
/// to say that related applications should be preferred over the web application.
/// </param>
/// <param name="shortcuts">
/// The `shortcuts` member is an `array` of `ShortcutItem`s that provide access to key tasks within a web application.
/// </param>
/// <see cref="https://w3c.github.io/manifest/#webappmanifest-dictionary"/>
public interface IWebAppManifest
{
    public TextDirectionType? Dir { get; set; }                                    /// <see cref="https://w3c.github.io/manifest/#dir-member"/>
    public string? Lang { get; set; }                                              /// <see cref="https://w3c.github.io/manifest/#lang-member"/>
    public string? Name { get; set; }                                              /// <see cref="https://w3c.github.io/manifest/#name-member"/>
    public string? ShortName { get; set; }                                         /// <see cref="https://w3c.github.io/manifest/#short_name-member"/>
    public string? Description { get; set; }                                       /// <see cref="https://w3c.github.io/manifest/#description-member"/>
    public List<ImageResource>? Icons { get; set; }                                  /// <see cref="https://w3c.github.io/manifest/#icons-member"/>
    public List<ImageResource>? Screenshots { get; set; }                            /// <see cref="https://w3c.github.io/manifest/#screenshots-member"/>
    public List<string>? Categories { get; set; }                                  /// <see cref="https://w3c.github.io/manifest/#categories-member"/>
    public string? IarcRatingId { get; set; }                                      /// <see cref="https://w3c.github.io/manifest/#iarc_rating_id-member"/>
    public string? StartUrl { get; set; }                                          /// <see cref="https://w3c.github.io/manifest/#start_url-member"/>
    public DisplayModeType? Display { get; set; }                                  /// <see cref="https://w3c.github.io/manifest/#display-member"/>
    public OrientationType? Orientation { get; set; }                              /// <see cref="https://w3c.github.io/manifest/#orientation-member"/>
    public string? Id { get; set; }                                                /// <see cref="https://w3c.github.io/manifest/#id-member"/>
    public string? ThemeColor { get; set; }                                        /// <see cref="https://w3c.github.io/manifest/#theme_color-member"/>
    public string? BackgroundColor { get; set; }                                   /// <see cref="https://w3c.github.io/manifest/#background_color-member"/>
    public string? Scope { get; set; }                                             /// <see cref="https://w3c.github.io/manifest/#scope-member"/>
    public List<ExternalApplicationResource>? RelatedApplications { get; set; }    /// <see cref="https://w3c.github.io/manifest/#related_applications-member"/>
    public bool? PreferRelatedApplications { get; set; }                           /// <see cref="https://w3c.github.io/manifest/#prefer_related_applications-member"/>
    public List<ShortcutItem>? Shortcuts { get; set; }                             /// <see cref="https://w3c.github.io/manifest/#shortcuts-member"/>
}
