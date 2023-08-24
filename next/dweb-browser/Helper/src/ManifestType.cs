
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Text.RegularExpressions;

namespace DwebBrowser.Helper;


#region ImageResource
/// <summary>
/// Each `ImageResource` represents an image that is used as part of a web application, suitable to use in
/// various contexts depending on the semantics of the member that is using the object (e.g., an icon
/// that is part of an application menu, etc.).
/// </summary>
/// <see cref="https://w3c.github.io/manifest/#imageresource-and-its-members"/>
public class ImageResource : IEquatable<ImageResource>
{
    [Obsolete("使用带参数的构造函数", true)]
#pragma warning disable CS8618 // 在退出构造函数时，不可为 null 的字段必须包含非 null 值。请考虑声明为可以为 null。
    public ImageResource()
#pragma warning restore CS8618 // 在退出构造函数时，不可为 null 的字段必须包含非 null 值。请考虑声明为可以为 null。
    {
        /// 给JSON反序列化用的空参数构造函数
    }

    /// <summary>
    /// The `src` member of an `ImageResource` is a URL from which a user agent can fetch the image's data.
    /// </summary>
    /// <see cref="https://w3c.github.io/manifest/#src-member"/>
    [JsonPropertyName("src")]
    public string Src { get; set; }

    /// <summary>
    /// The `sizes` member of an ImageResource is a string consisting of an unordered set of unique space-
    /// separated tokens which are ASCII case-insensitive that represents the dimensions of an image.
    /// </summary>
    /// <see cref="https://w3c.github.io/manifest/#sizes-member"/>
    [JsonPropertyName("sizes")]
    public string? Sizes { get; set; }

    /// <summary>
    /// The `type` member of an `ImageResource` is a hint as to the MIME type of the image.
    /// </summary>
    /// <see cref="https://w3c.github.io/manifest/#type-member"/>
    [JsonPropertyName("type")]
    public string? Type { get; set; }

    /// <summary>
    /// The purpose member is an unordered set of unique space-separated tokens that are ASCII case-
    /// insensitive.
    /// </summary>
    /// <see cref="https://w3c.github.io/manifest/#purpose-member"/>
    [JsonPropertyName("purpose")]
    public string? Purpose { get; set; }

    /// <summary>
    /// The `platform` member represents the platform to which a containing object applies.
    /// </summary>
    /// <see cref="https://w3c.github.io/manifest/#platform-member"/>
    [JsonPropertyName("platform")]
    public string? Platform { get; set; }

    public ImageResource(string src, string? sizes = null, string? type = null, string? purpose = null, string? platform = null)
    {
        Src = src;
        Sizes = sizes;
        Type = type;
        Purpose = purpose;
        Platform = platform;
    }

    public string ToJson() => JsonSerializer.Serialize(this);
    public static ImageResource? FromJson(string json) =>
        JsonSerializer.Deserialize<ImageResource>(json);

    public bool Equals(ImageResource? other)
    {
        return GetHashCode() == other?.GetHashCode();
    }

    public override int GetHashCode()
    {
        return Src.GetHashCode() ^
            Sizes?.GetHashCode() ?? 0 ^
            Type?.GetHashCode() ?? 0 ^
            Purpose?.GetHashCode() ?? 0 ^
            Platform?.GetHashCode() ?? 0;
    }
}
#endregion

public class ImageResourcePurposes
{
    public string Purpose { get; init; }
    public ImageResourcePurposes(string purpose)
    {
        Purpose = purpose;
    }

    public static readonly ImageResourcePurposes Monochrome = new("monochrome");
    public static readonly ImageResourcePurposes Maskable = new("maskable");
    public static readonly ImageResourcePurposes Any = new("any");

    public static ImageResourcePurposes? Find(Func<string, bool> find)
    {
        var infos = typeof(ImageResourcePurposes).GetProperties(System.Reflection.BindingFlags.Static);

        foreach (var info in infos)
        {
            var name = info.Name.ToLower();
            if (find(name))
            {
                return new ImageResourcePurposes(name);
            }
        }

        return null;
    }
}

public record ImageResourceSize(int Width, int Height);

public partial class StrictImageResource
{
    public string Src { get; init; }
    public HashSet<ImageResourcePurposes> Purpose { get; init; }
    public string Type { get; init; }
    public List<ImageResourceSize> Sizes { get; init; }


    public StrictImageResource(string src, HashSet<ImageResourcePurposes> purpose, string type, List<ImageResourceSize> sizes)
    {
        Src = src;
        Purpose = purpose;
        Type = type;
        Sizes = sizes;
    }

    public static StrictImageResource From(ImageResource img, string? baseUrl = null)
    {
        Uri imgUrl = baseUrl is not null ? new Uri(new Uri(baseUrl), img.Src) : new Uri(img.Src);
        var imageType = img.Type;

        if (imageType is null)
        {
            /// path的获取解析可能会失败
            var imageUrlExt = Path.GetExtension(imgUrl.AbsolutePath);
            imageType = imageUrlExt switch
            {
                "jpg" or "jpeg" => "image/jpeg",
                "webp" or "png" or "avif" or "apng" => $"image/{imageType}",
                "svg" => "image/svg+xml",
                _ => "image/*"
            };
        }

        var imageSizes = new List<ImageResourceSize>();
        if (img.Sizes is null)
        {
            if (imageType == "image/svg+xml")
            {
                imageSizes.Add(new ImageResourceSize(46340, 46340));
            }
            else
            {
                imageSizes.Add(new ImageResourceSize(1, 1));
            }
        }
        else if (img.Sizes == "any")
        {
            imageSizes.Add(new ImageResourceSize(46340, 46340));
        }
        else
        {
            
            var sizes = SpaceRegex().Split(img.Sizes);

            foreach (var size in sizes)
            {
                var match = IntRegex().Match(size);
                if (match.Success)
                {
                    var width = 0;
                    var height = 0;

                    if (int.TryParse(match.Groups[1].Value, out var w))
                    {
                        width = w;
                    }

                    if (int.TryParse(match.Groups[2].Value, out var h))
                    {
                        height = h;
                    }

                    imageSizes.Add(new ImageResourceSize(width, height));
                }
            }

            if (imageSizes.Count == 0)
            {
                imageSizes.Add(new ImageResourceSize(1, 1));
            }
        }

        return new StrictImageResource(
            imgUrl.ToString(),
            img.Purpose?.Let(purpose =>
            {
                var purposes = SpaceRegex().Split(purpose);

                var purposeList = purposes
                    .Select(keyword => ImageResourcePurposes.Find(p => p == keyword))
                    .Where(p => p != null)
                    .ToList();

                if (purposeList.Count > 0)
                {
                    return purposeList.Distinct().ToHashSet();
                }

                return null;
            }) ?? new HashSet<ImageResourcePurposes>() { ImageResourcePurposes.Any },
            imageType,
            imageSizes);
    }

    [GeneratedRegex(@"\s+")]
    private static partial Regex SpaceRegex();
    [GeneratedRegex(@"(\d+)x(\d+)")]
    private static partial Regex IntRegex();
}

