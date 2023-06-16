using System.Text;

namespace DwebBrowser.Helper;

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


    public static Span<T> Append<T>(this Span<T> first, Span<T> second)
    {
        if (second.Length == 0) return first;
        int originalLength = first.Length;
        T[] result = new T[first.Length + second.Length];
        first.CopyTo(result);
        second.CopyTo(result.AsSpan(originalLength));
        return result.AsSpan();
    }
    public static Span<T> Append<T>(this Span<T> first, T[] second) => first.Append(second.AsSpan());
    public static Span<T> Append<T>(this Span<T> first, ReadOnlySpan<T> second) => first.Append(second.AsSpan());
    public static ReadOnlySpan<T> AsSpan<T>(this ReadOnlySpan<T> self) {
        var result = new Span<T>(GC.AllocateUninitializedArray<T>(self.Length));
        self.CopyTo(result);
        return result;
    }

    /// <summary>
    /// concat two byte[]
    /// </summary>
    /// <param name="message">second byteArray</param>
    /// <returns></returns>
    /// https://stackoverflow.com/questions/415291/best-way-to-combine-two-or-more-byte-arrays-in-c-sharp
    public static byte[] Combine(this byte[] self, params byte[][] arrays)
    {
        var list = arrays.ToList();
        list.Insert(0, self);

        byte[] result = new byte[list.Sum(a => a.Length)];
        int offset = 0;
        foreach (byte[] array in list)
        {
            Buffer.BlockCopy(array, 0, result, offset, array.Length);
            offset += array.Length;
        }
        return result;
    }
    public static byte[] Combine(this byte[] self, Span<byte> span)
    {
        return self.AsSpan().Append(span).ToArray();
    }
    public static byte[] Combine(this byte[] self, byte[] bytes)
    {
        return self.Combine(bytes.AsSpan());
    }
    public static byte[] Combine(this byte[] self, ReadOnlySpan<byte> rspan)
    {
        return self.Combine(rspan.AsSpan());
    }
}