using System.Text;
using System.Web;
namespace DwebBrowser.Helper;

public static class StringExtensions
{
    public static byte[] FromBase64(this string self) => Convert.FromBase64String(self);

    public static byte[] FromUtf8(this string self) => Encoding.UTF8.GetBytes(self);

    public static int? ToIntOrNull(this string self) => Int32.TryParse(self, out int value) ? value : null;

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

