
namespace ipc.extensions;

public static class StringExtensions
{
    public static byte[] FromBase64(this string self) => Convert.FromBase64String(self);

    public static byte[] FromUtf8(this string self) => System.Text.UTF8Encoding.UTF8.GetBytes(self);
}

