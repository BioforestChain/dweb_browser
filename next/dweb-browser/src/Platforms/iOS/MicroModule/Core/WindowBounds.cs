using System.Text.Json.Serialization;

namespace DwebBrowser.MicroService.Core;

/// <summary>
/// 窗口大小与位置
///
/// 默认值是NaN，这种情况下，窗口构建者需要自己对其进行赋值
/// </summary>
/// <param name="left"></param>
/// <param name="top"></param>
/// <param name="width"></param>
/// <param name="height"></param>
public class WindowBounds : ICloneable
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

    public object Clone()
    {
        return MemberwiseClone();
    }
}

