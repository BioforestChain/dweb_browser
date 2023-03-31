using System.Text;

namespace micro_service.extensions;

public static class ByteArrayExtensions
{
    public static string ToUtf8(this byte[] self) =>
        Encoding.UTF8.GetString(self);
    public static string ToUtf8(this byte[] self, int offset, int count) =>
        Encoding.UTF8.GetString(self, offset, count);

    public static string ToBase64(this byte[] self) =>
        Convert.ToBase64String(self);

    public static int ToInt(this byte[] self) =>
        BitConverter.ToInt32(self, 0);


    /// <summary>
    /// concat two byte[]
    /// </summary>
    /// <param name="message">second byteArray</param>
    /// <returns></returns>
    /// https://stackoverflow.com/questions/415291/best-way-to-combine-two-or-more-byte-arrays-in-c-sharp
    public static byte[] Combine(this byte[] self, params byte[][] arrays) {
        byte[] rv = new byte[arrays.Sum(a => a.Length)];
        int offset = 0;
        foreach (byte[] array in arrays)
        {
            Buffer.BlockCopy(array, 0, rv, offset, array.Length);
            offset += array.Length;
        }
        return rv;
    }
}