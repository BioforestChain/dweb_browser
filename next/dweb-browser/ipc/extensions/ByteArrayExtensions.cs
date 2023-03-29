using System.Collections;
using System.Linq;

namespace ipc.extensions;

public static class ByteArrayExtensions
{
    public static string ToUtf8(this byte[] self) =>
        System.Text.UTF8Encoding.UTF8.GetString(self);
    public static string ToUtf8(this byte[] self, int offset, int count) =>
        System.Text.UTF8Encoding.UTF8.GetString(self, offset, count);

    public static string ToBase64(this byte[] self) =>
        Convert.ToBase64String(self);

    public static int ToInt(this byte[] self) =>
        BitConverter.ToInt32(self, 0);


    /// <summary>
    /// concat two byte[]
    /// </summary>
    /// <param name="message">second byteArray</param>
    /// <returns></returns>
    public static byte[] Combine(this byte[] self, byte[] message) =>
        self.Concat(message).ToArray();


}