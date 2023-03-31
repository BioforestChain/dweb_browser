using System.Text;
namespace micro_service.extensions;

public static class StringExtensions
{
    public static byte[] FromBase64(this string self) => Convert.FromBase64String(self);

    public static byte[] FromUtf8(this string self) => Encoding.UTF8.GetBytes(self);
}

