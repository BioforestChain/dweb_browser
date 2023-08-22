using UIKit;
namespace DwebBrowser.Helper;

public static class UIColorExtensions
{
    public static ColorJson ToColor(this UIColor self)
    {
        self.GetRGBA(out var red, out var green, out var blue, out var alpha);

        return new ColorJson(red * 255f, green * 255f, blue * 255f, alpha * 255f);
    }

    public static string ToCssRgba(this UIColor self)
    {
        self.GetRGBA(out var red, out var green, out var blue, out var alpha);

        return $"rgb({red * 255} {blue * 255} {green * 255}" + (alpha >= 1f ? ")" : $" / {alpha})");
    }

    public static string ToHex(this UIColor self, bool alphaBool = true)
    {
        self.GetRGBA(out var red, out var green, out var blue, out var alpha);

        return $"#{((float)red).To2Hex()}{((float)green).To2Hex()}{((float)blue).To2Hex()}" +
            (alphaBool && alpha < 1f ? ((float)alpha).To2Hex() : "");
    }
}
