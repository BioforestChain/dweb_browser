using System.Text;
using System.Web;

namespace DwebBrowser.Helper;

public static class StringExtensions
{
    public static byte[] ToBase64ByteArray(this string self) => Convert.FromBase64String(self);

    public static byte[] ToUtf8ByteArray(this string self) => Encoding.UTF8.GetBytes(self);

    public static int? ToIntOrNull(this string self) =>
        int.TryParse(self, out int value) ? value : null;

    public static long? ToLongOrNull(this string self) =>
        long.TryParse(self, out long value) ? value : null;

    public static float? ToFloatOrNull(this string self) =>
        float.TryParse(self, out float value) ? value : null;

    public static double? ToDoubleOrNull(this string self) =>
        double.TryParse(self, out double value) ? value : null;

    public static decimal? ToDecimalOrNull(this string self) =>
        decimal.TryParse(self, out decimal value) ? value : null;

    public static bool? ToBooleanStrictOrNull(this string self) => self switch
    {
        "true" => true,
        "false" => false,
        _ => null
    };

    public static string EncodeURI(this string self) => HttpUtility.UrlEncode(self);

    public static string DecodeURI(this string self) => HttpUtility.UrlDecode(self);

    public static string EncodeURIComponent(this string self) => Uri.EscapeDataString(self);

    public static string DecodeURIComponent(this string self) => Uri.UnescapeDataString(self);
}

